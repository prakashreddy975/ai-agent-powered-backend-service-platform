package com.aiplatform.project;

import com.aiplatform.common.exception.DuplicateResourceException;
import com.aiplatform.common.exception.ResourceNotFoundException;
import com.aiplatform.project.controller.ProjectController;
import com.aiplatform.project.dto.ProjectRequest;
import com.aiplatform.project.dto.ProjectResponse;
import com.aiplatform.project.model.ProjectStatus;
import com.aiplatform.project.service.ProjectService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProjectController.class)
class ProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProjectService projectService;

    private static ProjectResponse sample(long id) {
        Instant now = Instant.parse("2026-01-01T10:00:00Z");
        return new ProjectResponse(id, "Payments API", "Billing service", ProjectStatus.ACTIVE, now, now);
    }

    @Test
    void createReturns201WithLocationHeader() throws Exception {
        when(projectService.create(any(ProjectRequest.class))).thenReturn(sample(1));

        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Payments API", "description": "Billing service", "status": "ACTIVE"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/projects/1")))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.createdAt").value("2026-01-01T10:00:00Z"));
    }

    @Test
    void createWithBlankNameReturns400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "  ", "description": "%s"}
                                """.formatted("x".repeat(1001))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.fieldErrors.name").value("name is required"))
                .andExpect(jsonPath("$.fieldErrors.description").value("description must be at most 1000 characters"));

        verifyNoInteractions(projectService);
    }

    @Test
    void createWithUnknownStatusReturns400() throws Exception {
        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "A", "status": "DONE"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request body is missing or malformed"));
    }

    @Test
    void createDuplicateReturns409() throws Exception {
        when(projectService.create(any())).thenThrow(new DuplicateResourceException("A project named 'A' already exists"));

        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"A\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("A project named 'A' already exists"));
    }

    @Test
    void findAllReturnsList() throws Exception {
        when(projectService.findAll(null)).thenReturn(List.of(sample(1), sample(2)));

        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void findAllPassesStatusFilter() throws Exception {
        when(projectService.findAll(ProjectStatus.ACTIVE)).thenReturn(List.of(sample(1)));

        mockMvc.perform(get("/api/projects").param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void findByIdNotFoundReturns404() throws Exception {
        when(projectService.findById(99L)).thenThrow(new ResourceNotFoundException("Project with id 99 was not found"));

        mockMvc.perform(get("/api/projects/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.path").value("/api/projects/99"));
    }

    @Test
    void nonNumericIdReturns400() throws Exception {
        mockMvc.perform(get("/api/projects/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value 'abc' for parameter 'id'"));
    }

    @Test
    void updateReturns200() throws Exception {
        when(projectService.update(eq(1L), any(ProjectRequest.class))).thenReturn(sample(1));

        mockMvc.perform(put("/api/projects/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Payments API\", \"status\": \"ACTIVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Payments API"));
    }

    @Test
    void deleteReturns204() throws Exception {
        mockMvc.perform(delete("/api/projects/1")).andExpect(status().isNoContent());
        verify(projectService).delete(1L);
    }

    @Test
    void deleteMissingReturns404() throws Exception {
        doThrow(new ResourceNotFoundException("Project with id 7 was not found")).when(projectService).delete(7L);

        mockMvc.perform(delete("/api/projects/7")).andExpect(status().isNotFound());
    }

    @Test
    void unexpectedErrorReturns500WithoutInternals() throws Exception {
        when(projectService.findById(1L)).thenThrow(new IllegalStateException("db password is hunter2"));

        mockMvc.perform(get("/api/projects/1"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"));
    }
}
