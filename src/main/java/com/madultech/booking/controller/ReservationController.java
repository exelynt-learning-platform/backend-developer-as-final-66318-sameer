package com.madultech.booking.controller;

import com.madultech.booking.dto.reservation.ReservationCreateRequest;
import com.madultech.booking.dto.reservation.ReservationResponse;
import com.madultech.booking.dto.reservation.ReservationStatusUpdateRequest;
import com.madultech.booking.dto.reservation.ReservationUpdateRequest;
import com.madultech.booking.entity.User;
import com.madultech.booking.enums.ReservationStatus;
import com.madultech.booking.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
@Tag(name = "Reservations", description = "Bookings against resources")
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping
    @Operation(summary = "Create a reservation — owner is taken from the JWT, never from the request body")
    public ResponseEntity<ReservationResponse> create(@Valid @RequestBody ReservationCreateRequest request,
                                                        @AuthenticationPrincipal User currentUser) {
        ReservationResponse created = reservationService.createReservation(request, currentUser);
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
        return ResponseEntity.ok(reservationService.getReservations(currentUser, status, minPrice, maxPrice, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single reservation — ADMIN or the owning USER only")
    public ResponseEntity<ReservationResponse> getById(@PathVariable Long id,
                                                         @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(reservationService.getReservationById(id, currentUser));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Fully update a reservation — ADMIN only")
    public ResponseEntity<ReservationResponse> update(@PathVariable Long id,
                                                        @Valid @RequestBody ReservationUpdateRequest request,
                                                        @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(reservationService.updateReservation(id, request, currentUser));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update reservation status — ADMIN can set any status, USER can only cancel their own")
    public ResponseEntity<ReservationResponse> updateStatus(@PathVariable Long id,
                                                              @Valid @RequestBody ReservationStatusUpdateRequest request,
                                                              @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(reservationService.updateStatus(id, request, currentUser));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a reservation — ADMIN only")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal User currentUser) {
        reservationService.deleteReservation(id, currentUser);
        return ResponseEntity.noContent().build();
    }
}
