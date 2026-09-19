package com.sameer.booking.dto.reservation;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Request body for creating a reservation.
 * Note: there is intentionally NO userId field here — the owning user is
 * always resolved from the authenticated JWT principal in the service layer,
 * never trusted from client input.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReservationCreateRequest {

    @NotNull(message = "Resource id is required")
    private Long resourceId;

    @NotNull(message = "Start time is required")
    @Future(message = "Start time must be in the future")
    private LocalDateTime startTime;

    @NotNull(message = "End time is required")
    @Future(message = "End time must be in the future")
    private LocalDateTime endTime;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Price must not be negative")
    private BigDecimal price;
}
