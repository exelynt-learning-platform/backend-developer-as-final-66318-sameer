package com.sameer.booking.controller;

import com.sameer.booking.dto.reservation.ReservationCreateRequest;
import com.sameer.booking.dto.reservation.ReservationStatusUpdateRequest;
import com.sameer.booking.entity.Reservation;
import com.sameer.booking.enums.ReservationStatus;
import com.sameer.booking.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ReservationControllerTest extends IntegrationTestSupport {

    private ReservationCreateRequest sampleCreateRequest(BigDecimal price) {
        ReservationCreateRequest request = new ReservationCreateRequest();
        request.setResourceId(sampleResource.getId());
        request.setStartTime(LocalDateTime.now().plusDays(1));
        request.setEndTime(LocalDateTime.now().plusDays(1).plusHours(2));
        request.setPrice(price);
        return request;
    }

    @Test
    void userCreatingReservationIsAlwaysOwnedByTheAuthenticatedCaller() throws Exception {
        mockMvc.perform(post("/api/reservations")
                        .header(HttpHeaders.AUTHORIZATION, bearer(userToken))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(sampleCreateRequest(new BigDecimal("50.00")))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void endTimeBeforeStartTimeIsRejected() throws Exception {
        ReservationCreateRequest request = sampleCreateRequest(new BigDecimal("50.00"));
        request.setEndTime(request.getStartTime().minusHours(5));

        mockMvc.perform(post("/api/reservations")
                        .header(HttpHeaders.AUTHORIZATION, bearer(userToken))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void negativePriceIsRejectedByValidation() throws Exception {
        ReservationCreateRequest request = sampleCreateRequest(new BigDecimal("-10.00"));

        mockMvc.perform(post("/api/reservations")
                        .header(HttpHeaders.AUTHORIZATION, bearer(userToken))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void userSeesOnlyOwnReservationsWhileAdminSeesAll() throws Exception {
        Reservation aliceRes = reservationRepository.save(Reservation.builder()
                .resource(sampleResource).user(regularUser)
                .startTime(LocalDateTime.now().plusDays(1)).endTime(LocalDateTime.now().plusDays(1).plusHours(1))
                .price(new BigDecimal("30.00")).status(ReservationStatus.PENDING).build());

        Reservation bobRes = reservationRepository.save(Reservation.builder()
                .resource(sampleResource).user(secondRegularUser)
                .startTime(LocalDateTime.now().plusDays(2)).endTime(LocalDateTime.now().plusDays(2).plusHours(1))
                .price(new BigDecimal("40.00")).status(ReservationStatus.PENDING).build());

        mockMvc.perform(get("/api/reservations").header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(aliceRes.getId()));

        mockMvc.perform(get("/api/reservations").header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    void userCannotViewAnotherUsersReservationById() throws Exception {
        Reservation bobRes = reservationRepository.save(Reservation.builder()
                .resource(sampleResource).user(secondRegularUser)
                .startTime(LocalDateTime.now().plusDays(2)).endTime(LocalDateTime.now().plusDays(2).plusHours(1))
                .price(new BigDecimal("40.00")).status(ReservationStatus.PENDING).build());

        mockMvc.perform(get("/api/reservations/" + bobRes.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/reservations/" + bobRes.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk());
    }

    @Test
    void filteringByStatusAndPriceRangeWorks() throws Exception {
        reservationRepository.save(Reservation.builder()
                .resource(sampleResource).user(regularUser)
                .startTime(LocalDateTime.now().plusDays(1)).endTime(LocalDateTime.now().plusDays(1).plusHours(1))
                .price(new BigDecimal("20.00")).status(ReservationStatus.PENDING).build());
        reservationRepository.save(Reservation.builder()
                .resource(sampleResource).user(regularUser)
                .startTime(LocalDateTime.now().plusDays(1)).endTime(LocalDateTime.now().plusDays(1).plusHours(1))
                .price(new BigDecimal("80.00")).status(ReservationStatus.CONFIRMED).build());
        reservationRepository.save(Reservation.builder()
                .resource(sampleResource).user(regularUser)
                .startTime(LocalDateTime.now().plusDays(1)).endTime(LocalDateTime.now().plusDays(1).plusHours(1))
                .price(new BigDecimal("150.00")).status(ReservationStatus.CONFIRMED).build());

        mockMvc.perform(get("/api/reservations")
                        .header(HttpHeaders.AUTHORIZATION, bearer(userToken))
                        .param("status", "CONFIRMED")
                        .param("minPrice", "50")
                        .param("maxPrice", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].price").value(80.00));
    }

    @Test
    void paginationParametersAreRespected() throws Exception {
        for (int i = 0; i < 5; i++) {
            reservationRepository.save(Reservation.builder()
                    .resource(sampleResource).user(regularUser)
                    .startTime(LocalDateTime.now().plusDays(1)).endTime(LocalDateTime.now().plusDays(1).plusHours(1))
                    .price(new BigDecimal("10.00")).status(ReservationStatus.PENDING).build());
        }

        mockMvc.perform(get("/api/reservations")
                        .header(HttpHeaders.AUTHORIZATION, bearer(userToken))
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(5));
    }

    @Test
    void userCanCancelOwnReservationButNotConfirmIt() throws Exception {
        Reservation res = reservationRepository.save(Reservation.builder()
                .resource(sampleResource).user(regularUser)
                .startTime(LocalDateTime.now().plusDays(1)).endTime(LocalDateTime.now().plusDays(1).plusHours(1))
                .price(new BigDecimal("10.00")).status(ReservationStatus.PENDING).build());

        ReservationStatusUpdateRequest confirmRequest = new ReservationStatusUpdateRequest(ReservationStatus.CONFIRMED);
        mockMvc.perform(patch("/api/reservations/" + res.getId() + "/status")
                        .header(HttpHeaders.AUTHORIZATION, bearer(userToken))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(confirmRequest)))
                .andExpect(status().isForbidden());

        ReservationStatusUpdateRequest cancelRequest = new ReservationStatusUpdateRequest(ReservationStatus.CANCELLED);
        mockMvc.perform(patch("/api/reservations/" + res.getId() + "/status")
                        .header(HttpHeaders.AUTHORIZATION, bearer(userToken))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cancelRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void userCannotDeleteReservationOnlyAdminCan() throws Exception {
        Reservation res = reservationRepository.save(Reservation.builder()
                .resource(sampleResource).user(regularUser)
                .startTime(LocalDateTime.now().plusDays(1)).endTime(LocalDateTime.now().plusDays(1).plusHours(1))
                .price(new BigDecimal("10.00")).status(ReservationStatus.PENDING).build());

        mockMvc.perform(delete("/api/reservations/" + res.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/reservations/" + res.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isNoContent());
    }
}
