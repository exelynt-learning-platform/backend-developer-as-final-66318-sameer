package com.sameer.booking.service;

import com.sameer.booking.dto.reservation.ReservationCreateRequest;
import com.sameer.booking.dto.reservation.ReservationResponse;
import com.sameer.booking.dto.reservation.ReservationStatusUpdateRequest;
import com.sameer.booking.dto.reservation.ReservationUpdateRequest;
import com.sameer.booking.entity.Reservation;
import com.sameer.booking.entity.Resource;
import com.sameer.booking.entity.User;
import com.sameer.booking.enums.ReservationStatus;
import com.sameer.booking.exception.AccessDeniedCustomException;
import com.sameer.booking.exception.InvalidReservationException;
import com.sameer.booking.exception.ResourceNotFoundException;
import com.sameer.booking.repository.ReservationRepository;
import com.sameer.booking.repository.UserRepository;
import com.sameer.booking.specification.ReservationSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ResourceService resourceService;
    private final UserRepository userRepository;

    public ReservationService(ReservationRepository reservationRepository,
                              ResourceService resourceService,
                              UserRepository userRepository) {
        this.reservationRepository = reservationRepository;
        this.resourceService = resourceService;
        this.userRepository = userRepository;
    }


    @Transactional
    public ReservationResponse createReservation(ReservationCreateRequest request, Long currentUserId) {
        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new InvalidReservationException("End time must be after start time");
        }

        Resource resource = resourceService.findByIdOrThrow(request.getResourceId());
        if (!resource.isAvailable()) {
            throw new InvalidReservationException("Resource is not available for booking");
        }


        User owner = userRepository.getReferenceById(currentUserId);

        Reservation reservation = Reservation.builder()
                .resource(resource)
                .user(owner)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .price(request.getPrice())
                .status(ReservationStatus.PENDING)
                .build();

        return toResponse(reservationRepository.save(reservation));
    }

    @Transactional(readOnly = true)
    public Page<ReservationResponse> getReservations(Long currentUserId,
                                                     boolean isAdmin,
                                                     ReservationStatus status,
                                                     BigDecimal minPrice,
                                                     BigDecimal maxPrice,
                                                     Pageable pageable) {
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new InvalidReservationException("minPrice cannot be greater than maxPrice");
        }

        // ADMIN sees every reservation; USER is scoped to their own regardless of what they ask for.
        Long ownerFilter = isAdmin ? null : currentUserId;

        Specification<Reservation> spec = Specification
                .where(ReservationSpecification.ownedBy(ownerFilter))
                .and(ReservationSpecification.hasStatus(status))
                .and(ReservationSpecification.minPrice(minPrice))
                .and(ReservationSpecification.maxPrice(maxPrice));

        return reservationRepository.findAll(spec, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ReservationResponse getReservationById(Long id, Long currentUserId, boolean isAdmin) {
        Reservation reservation = findByIdOrThrow(id);
        assertOwnershipOrAdmin(reservation, currentUserId, isAdmin,
                "You may only view your own reservations");
        return toResponse(reservation);
    }

    @Transactional
    public ReservationResponse updateReservation(Long id, ReservationUpdateRequest request, boolean isAdmin) {
        if (!isAdmin) {
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
    public ReservationResponse updateStatus(Long id, ReservationStatusUpdateRequest request,
                                            Long currentUserId, boolean isAdmin) {
        Reservation reservation = findByIdOrThrow(id);
        boolean isOwner = reservation.getUser().getId().equals(currentUserId);

        if (!isAdmin && !isOwner) {
            throw new AccessDeniedCustomException("You may only modify your own reservations");
        }

        if (!isAdmin && request.getStatus() != ReservationStatus.CANCELLED) {
            throw new AccessDeniedCustomException(
                    "You may only cancel your own reservation — other status changes require an administrator");
        }

        reservation.setStatus(request.getStatus());
        return toResponse(reservationRepository.save(reservation));
    }

    @Transactional
    public void deleteReservation(Long id, boolean isAdmin) {
        Reservation reservation = findByIdOrThrow(id);
        // Deletion is destructive — restrict it to ADMIN even for the reservation's own
        // owner; a USER cancels via updateStatus instead of deleting the record.
        if (!isAdmin) {
            throw new AccessDeniedCustomException("Only administrators can delete reservations");
        }
        reservationRepository.delete(reservation);
    }

    Reservation findByIdOrThrow(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with id: " + id));
    }

    private void assertOwnershipOrAdmin(Reservation reservation, Long currentUserId, boolean isAdmin, String message) {
        boolean owner = reservation.getUser().getId().equals(currentUserId);
        if (!isAdmin && !owner) {
            throw new AccessDeniedCustomException(message);
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
}