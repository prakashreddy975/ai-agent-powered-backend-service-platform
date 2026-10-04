package com.aiplatform.project;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-stack CRUD flow (controller → service → repository → Flyway schema) against
 * an in-memory H2 database. Also proves the application starts without ANTHROPIC_API_KEY.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ProjectApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void crudLifecycle() throws Exception {
        String body = mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Inventory Service\", \"description\": \"Stock tracking\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PLANNED"))
                .andReturn().getResponse().getContentAsString();
        JsonNode created = objectMapper.readTree(body);
        long id = created.get("id").asLong();

        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"inventory service\"}"))
                .andExpect(status().isConflict());

        mockMvc.perform(put("/api/projects/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Inventory Service\", \"description\": \"v2\", \"status\": \"ACTIVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.description").value("v2"));

        mockMvc.perform(get("/api/projects").param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id));

        mockMvc.perform(delete("/api/projects/" + id)).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/projects/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void aiEndpointReportsMissingConfiguration() throws Exception {
        mockMvc.perform(post("/api/ai/code-review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"language\": \"java\", \"code\": \"class A {}\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("ANTHROPIC_API_KEY")));
    }
}
