package com.sameer.booking.controller;

import com.sameer.booking.dto.reservation.ReservationCreateRequest;
import com.sameer.booking.dto.reservation.ReservationResponse;
import com.sameer.booking.dto.reservation.ReservationStatusUpdateRequest;
import com.sameer.booking.dto.reservation.ReservationUpdateRequest;
import com.sameer.booking.entity.User;
import com.sameer.booking.enums.ReservationStatus;
import com.sameer.booking.enums.Role;
import com.sameer.booking.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/reservations")
@Tag(name = "Reservations", description = "Bookings against resources")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
    @Operation(summary = "Create a reservation — owner is taken from the JWT, never from the request body")
    public ResponseEntity<ReservationResponse> create(@Valid @RequestBody ReservationCreateRequest request,
                                                      @AuthenticationPrincipal User currentUser) {
        ReservationResponse created = reservationService.createReservation(request, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    @Operation(summary = "List reservations — ADMIN sees all, USER sees only their own. " +
            "Supports filtering by status/minPrice/maxPrice, pagination, and sorting.")
    public ResponseEntity<Page<ReservationResponse>> getAll(
            @AuthenticationPrincipal User currentUser,
            @Parameter(description = "Filter by reservation status") @RequestParam(required = false) ReservationStatus status,
            @Parameter(description = "Minimum price (inclusive)") @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "Maximum price (inclusive)") @RequestParam(required = false) BigDecimal maxPrice,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(reservationService.getReservations(
                currentUser.getId(), isAdmin(currentUser), status, minPrice, maxPrice, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single reservation — ADMIN or the owning USER only")
    public ResponseEntity<ReservationResponse> getById(@PathVariable Long id,
                                                       @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(
                reservationService.getReservationById(id, currentUser.getId(), isAdmin(currentUser)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Fully update a reservation — ADMIN only")
    public ResponseEntity<ReservationResponse> update(@PathVariable Long id,
                                                      @Valid @RequestBody ReservationUpdateRequest request,
                                                      @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(reservationService.updateReservation(id, request, isAdmin(currentUser)));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update reservation status — ADMIN can set any status, USER can only cancel their own")
    public ResponseEntity<ReservationResponse> updateStatus(@PathVariable Long id,
                                                            @Valid @RequestBody ReservationStatusUpdateRequest request,
                                                            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(reservationService.updateStatus(
                id, request, currentUser.getId(), isAdmin(currentUser)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a reservation — ADMIN only")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal User currentUser) {
        reservationService.deleteReservation(id, isAdmin(currentUser));
        return ResponseEntity.noContent().build();
    }

    private boolean isAdmin(User user) {
        return user.getRole() == Role.ADMIN;
    }
}