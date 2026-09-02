package com.madultech.booking.controller;

import com.madultech.booking.dto.resource.ResourceRequest;
import com.madultech.booking.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ResourceControllerTest extends IntegrationTestSupport {

    @Test
    void userCanListResources() throws Exception {
        mockMvc.perform(get("/api/resources")
                        .header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        mockMvc.perform(get("/api/resources"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void userCannotCreateResource() throws Exception {
        ResourceRequest request = new ResourceRequest("New Room", "ROOM", "desc", "Floor 3", true);

        mockMvc.perform(post("/api/resources")
                        .header(HttpHeaders.AUTHORIZATION, bearer(userToken))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanCreateResource() throws Exception {
        ResourceRequest request = new ResourceRequest("New Room", "ROOM", "desc", "Floor 3", true);

        mockMvc.perform(post("/api/resources")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("New Room"));
    }

    @Test
    void adminCanUpdateAndDeleteResource() throws Exception {
        ResourceRequest updateRequest = new ResourceRequest("Updated Room", "ROOM", "updated desc", "Floor 4", false);

        mockMvc.perform(put("/api/resources/" + sampleResource.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Room"))
                .andExpect(jsonPath("$.available").value(false));

        mockMvc.perform(delete("/api/resources/" + sampleResource.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isNoContent());
    }

    @Test
    void userCannotDeleteResource() throws Exception {
        mockMvc.perform(delete("/api/resources/" + sampleResource.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void gettingUnknownResourceReturns404() throws Exception {
        mockMvc.perform(get("/api/resources/999999")
                        .header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
                .andExpect(status().isNotFound());
    }
}
