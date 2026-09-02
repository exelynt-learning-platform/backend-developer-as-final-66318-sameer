package com.madultech.booking.service;

import com.madultech.booking.dto.reservation.ReservationCreateRequest;
import com.madultech.booking.dto.reservation.ReservationResponse;
import com.madultech.booking.dto.reservation.ReservationStatusUpdateRequest;
import com.madultech.booking.dto.reservation.ReservationUpdateRequest;
import com.madultech.booking.entity.Reservation;
import com.madultech.booking.entity.Resource;
import com.madultech.booking.entity.User;
import com.madultech.booking.enums.ReservationStatus;
import com.madultech.booking.exception.AccessDeniedCustomException;
import com.madultech.booking.exception.InvalidReservationException;
import com.madultech.booking.exception.ResourceNotFoundException;
import com.madultech.booking.repository.ReservationRepository;
import com.madultech.booking.specification.ReservationSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ResourceService resourceService;

    /**
     * Creates a reservation for the CURRENTLY AUTHENTICATED user.
     * The owning user is passed in explicitly from the controller (resolved
     * from the JWT principal) — it is never read from the request body.
     */
    @Transactional
    public ReservationResponse createReservation(ReservationCreateRequest request, User currentUser) {
        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new InvalidReservationException("End time must be after start time");
        }

        Resource resource = resourceService.findByIdOrThrow(request.getResourceId());
        if (!resource.isAvailable()) {
            throw new InvalidReservationException("Resource is not available for booking");
        }

        Reservation reservation = Reservation.builder()
                .resource(resource)
                .user(currentUser)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .price(request.getPrice())
                .status(ReservationStatus.PENDING)
                .build();

        return toResponse(reservationRepository.save(reservation));
    }

    @Transactional(readOnly = true)
    public Page<ReservationResponse> getReservations(User currentUser,
                                                       ReservationStatus status,
                                                       BigDecimal minPrice,
                                                       BigDecimal maxPrice,
                                                       Pageable pageable) {
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new InvalidReservationException("minPrice cannot be greater than maxPrice");
        }

        // ADMIN sees every reservation; USER is scoped to their own regardless of what they ask for.
        Long ownerFilter = SecurityRoleCheck.isAdmin(currentUser) ? null : currentUser.getId();

        Specification<Reservation> spec = Specification
                .where(ReservationSpecification.ownedBy(ownerFilter))
                .and(ReservationSpecification.hasStatus(status))
                .and(ReservationSpecification.minPrice(minPrice))
                .and(ReservationSpecification.maxPrice(maxPrice));

        return reservationRepository.findAll(spec, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ReservationResponse getReservationById(Long id, User currentUser) {
        Reservation reservation = findByIdOrThrow(id);
        assertOwnershipOrAdmin(reservation, currentUser);
        return toResponse(reservation);
    }

    @Transactional
    public ReservationResponse updateReservation(Long id, ReservationUpdateRequest request, User currentUser) {
        // Full update (including status/price/resource reassignment) is an ADMIN-only operation.
        if (!SecurityRoleCheck.isAdmin(currentUser)) {
            throw new AccessDeniedCustomException("Only administrators can fully update a reservation");
        }

        Reservation reservation = findByIdOrThrow(id);

        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new InvalidReservationException("End time must be after start time");
        }

        Resource resource = resourceService.findByIdOrThrow(request.getResourceId());

        reservation.setResource(resource);
        reservation.setStartTime(request.getStartTime());
        reservation.setEndTime(request.getEndTime());
        reservation.setStatus(request.getStatus());
        reservation.setPrice(request.getPrice());

        return toResponse(reservationRepository.save(reservation));
    }

    @Transactional
    public ReservationResponse updateStatus(Long id, ReservationStatusUpdateRequest request, User currentUser) {
        Reservation reservation = findByIdOrThrow(id);
        assertOwnershipOrAdmin(reservation, currentUser);

        // A USER may only cancel their own reservation — every other transition is ADMIN-only.
        boolean admin = SecurityRoleCheck.isAdmin(currentUser);
        if (!admin && request.getStatus() != ReservationStatus.CANCELLED) {
            throw new AccessDeniedCustomException("Users may only cancel their own reservations");
        }

        reservation.setStatus(request.getStatus());
        return toResponse(reservationRepository.save(reservation));
    }

    @Transactional
    public void deleteReservation(Long id, User currentUser) {
        Reservation reservation = findByIdOrThrow(id);
        // Deletion is destructive — restrict it to ADMIN even though USER owns the record;
        // USER should cancel via status update instead.
        if (!SecurityRoleCheck.isAdmin(currentUser)) {
            throw new AccessDeniedCustomException("Only administrators can delete reservations");
        }
        reservationRepository.delete(reservation);
    }

    Reservation findByIdOrThrow(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with id: " + id));
    }

    private void assertOwnershipOrAdmin(Reservation reservation, User currentUser) {
        boolean admin = SecurityRoleCheck.isAdmin(currentUser);
        boolean owner = reservation.getUser().getId().equals(currentUser.getId());
        if (!admin && !owner) {
            throw new AccessDeniedCustomException("You may only access your own reservations");
        }
    }

    private ReservationResponse toResponse(Reservation r) {
        return ReservationResponse.builder()
                .id(r.getId())
                .resourceId(r.getResource().getId())
                .resourceName(r.getResource().getName())
                .userId(r.getUser().getId())
                .username(r.getUser().getUsername())
                .startTime(r.getStartTime())
                .endTime(r.getEndTime())
                .status(r.getStatus())
                .price(r.getPrice())
                .createdAt(r.getCreatedAt())
                .build();
    }

    /** Small local helper so this class doesn't depend on the SecurityContext directly — easier to unit test. */
    static final class SecurityRoleCheck {
        static boolean isAdmin(User user) {
            return user.getRole().name().equals("ADMIN");
        }
    }
}
