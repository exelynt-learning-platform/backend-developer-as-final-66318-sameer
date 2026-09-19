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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
    public ReservationResponse createReservation(
            ReservationCreateRequest request,
            Long currentUserId) {

        validateTime(request.getStartTime(), request.getEndTime());

        Resource resource =
                resourceService.findByIdOrThrow(request.getResourceId());

        if (!resource.isAvailable()) {
            throw new InvalidReservationException(
                    "Resource is not available for booking");
        }

        User owner = userRepository.findById(currentUserId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        if (hasOverlappingReservation(
                resource.getId(),
                request.getStartTime(),
                request.getEndTime(),
                null)) {

            throw new InvalidReservationException(
                    "Resource is already booked for this time slot");
        }

        Reservation reservation = Reservation.builder()
                .resource(resource)
                .user(owner)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .price(request.getPrice())
                .status(ReservationStatus.PENDING)
                .build();

        return toResponse(
                reservationRepository.save(reservation)
        );
    }

    @Transactional(readOnly = true)
    public Page<ReservationResponse> getReservations(
            Long currentUserId,
            boolean isAdmin,
            ReservationStatus status,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Pageable pageable) {

        if (minPrice != null
                && maxPrice != null
                && minPrice.compareTo(maxPrice) > 0) {

            throw new InvalidReservationException(
                    "minPrice cannot be greater than maxPrice");
        }

        // ADMIN sees all reservations.
        // USER sees only their own reservations.
        Long ownerFilter = isAdmin ? null : currentUserId;

        Specification<Reservation> spec = Specification
                .where(ReservationSpecification.ownedBy(ownerFilter))
                .and(ReservationSpecification.hasStatus(status))
                .and(ReservationSpecification.minPrice(minPrice))
                .and(ReservationSpecification.maxPrice(maxPrice));

        return reservationRepository
                .findAll(spec, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ReservationResponse getReservationById(
            Long id,
            Long currentUserId,
            boolean isAdmin) {

        Reservation reservation = findByIdOrThrow(id);

        assertOwnershipOrAdmin(
                reservation,
                currentUserId,
                isAdmin,
                "You may only view your own reservations"
        );

        return toResponse(reservation);
    }

    @Transactional
    public ReservationResponse updateReservation(
            Long id,
            ReservationUpdateRequest request,
            boolean isAdmin) {

        if (!isAdmin) {
            throw new AccessDeniedCustomException(
                    "Only administrators can fully update a reservation");
        }

        Reservation reservation = findByIdOrThrow(id);

        validateTime(
                request.getStartTime(),
                request.getEndTime()
        );

        Resource resource =
                resourceService.findByIdOrThrow(request.getResourceId());

        if (!resource.isAvailable()
                && request.getStatus() != ReservationStatus.CANCELLED) {

            throw new InvalidReservationException(
                    "Resource is not available for booking");
        }

        /*
         * CANCELLED reservations do not reserve a time slot.
         * For other statuses, check whether another reservation
         * already occupies the requested time range.
         */
        if (request.getStatus() != ReservationStatus.CANCELLED
                && hasOverlappingReservation(
                resource.getId(),
                request.getStartTime(),
                request.getEndTime(),
                id)) {

            throw new InvalidReservationException(
                    "Resource is already booked for this time slot");
        }

        reservation.setResource(resource);
        reservation.setStartTime(request.getStartTime());
        reservation.setEndTime(request.getEndTime());
        reservation.setStatus(request.getStatus());
        reservation.setPrice(request.getPrice());

        return toResponse(
                reservationRepository.save(reservation)
        );
    }

    @Transactional
    public ReservationResponse updateStatus(
            Long id,
            ReservationStatusUpdateRequest request,
            Long currentUserId,
            boolean isAdmin) {

        Reservation reservation = findByIdOrThrow(id);

        boolean isOwner =
                reservation.getUser().getId().equals(currentUserId);

        if (!isAdmin && !isOwner) {
            throw new AccessDeniedCustomException(
                    "You may only modify your own reservations");
        }

        /*
         * USER can only cancel their own reservation.
         * Other status changes require ADMIN.
         */
        if (!isAdmin
                && request.getStatus() != ReservationStatus.CANCELLED) {

            throw new AccessDeniedCustomException(
                    "You may only cancel your own reservation - "
                            + "other status changes require an administrator"
            );
        }

        reservation.setStatus(request.getStatus());

        return toResponse(
                reservationRepository.save(reservation)
        );
    }

    @Transactional
    public void deleteReservation(Long id, boolean isAdmin) {

        Reservation reservation = findByIdOrThrow(id);

        if (!isAdmin) {
            throw new AccessDeniedCustomException(
                    "Only administrators can delete reservations");
        }

        reservationRepository.delete(reservation);
    }

    Reservation findByIdOrThrow(Long id) {

        return reservationRepository
                .findWithUserAndResourceById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Reservation not found with id: " + id
                        ));
    }

    private void assertOwnershipOrAdmin(
            Reservation reservation,
            Long currentUserId,
            boolean isAdmin,
            String message) {

        boolean owner =
                reservation.getUser().getId().equals(currentUserId);

        if (!isAdmin && !owner) {
            throw new AccessDeniedCustomException(message);
        }
    }

    private void validateTime(
            LocalDateTime startTime,
            LocalDateTime endTime) {

        if (!endTime.isAfter(startTime)) {
            throw new InvalidReservationException(
                    "End time must be after start time");
        }
    }

    /**
     * Checks whether another active reservation overlaps
     * with the requested time range for the same resource.
     */
    private boolean hasOverlappingReservation(
            Long resourceId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Long excludeReservationId) {

        Specification<Reservation> specification =
                (root, query, criteriaBuilder) -> {

                    List<jakarta.persistence.criteria.Predicate> predicates =
                            new ArrayList<>();

                    // Same resource
                    predicates.add(
                            criteriaBuilder.equal(
                                    root.get("resource").get("id"),
                                    resourceId
                            )
                    );

                    // Cancelled reservations don't block a slot
                    predicates.add(
                            criteriaBuilder.notEqual(
                                    root.get("status"),
                                    ReservationStatus.CANCELLED
                            )
                    );

                    // Existing start < new end
                    predicates.add(
                            criteriaBuilder.lessThan(
                                    root.get("startTime"),
                                    endTime
                            )
                    );

                    // Existing end > new start
                    predicates.add(
                            criteriaBuilder.greaterThan(
                                    root.get("endTime"),
                                    startTime
                            )
                    );

                    // When updating, ignore the current reservation itself
                    if (excludeReservationId != null) {
                        predicates.add(
                                criteriaBuilder.notEqual(
                                        root.get("id"),
                                        excludeReservationId
                                )
                        );
                    }

                    return criteriaBuilder.and(
                            predicates.toArray(
                                    new jakarta.persistence.criteria.Predicate[0]
                            )
                    );
                };

        return reservationRepository
                .findAll(specification, Pageable.ofSize(1))
                .hasContent();
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