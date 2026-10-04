package com.aiplatform.ai;

import com.aiplatform.ai.client.AiReviewClient;
import com.aiplatform.ai.controller.AiController;
import com.aiplatform.ai.dto.CodeReviewResponse;
import com.aiplatform.ai.dto.IssueSeverity;
import com.aiplatform.ai.dto.ReviewIssue;
import com.aiplatform.ai.service.CodeReviewService;
import com.aiplatform.common.exception.AiServiceException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises controller + real service with a mocked Claude client, so no real API calls are made.
 */
@WebMvcTest(AiController.class)
@Import(CodeReviewService.class)
class AiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AiReviewClient aiReviewClient;

    @Test
    void returnsStructuredReview() throws Exception {
        when(aiReviewClient.isConfigured()).thenReturn(true);
        when(aiReviewClient.reviewCode(any(), any())).thenReturn(new CodeReviewResponse(
                "Simple class with one bug.",
                List.of(new ReviewIssue(IssueSeverity.MEDIUM, "line 1", "Field is never used", "Remove it")),
                List.of("Make the class final")));

        mockMvc.perform(post("/api/ai/code-review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"language\": \"java\", \"code\": \"class A { int x; }\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary").value("Simple class with one bug."))
                .andExpect(jsonPath("$.issues[0].severity").value("MEDIUM"))
                .andExpect(jsonPath("$.recommendations[0]").value("Make the class final"));
    }

    @Test
    void missingCodeReturns400() throws Exception {
        mockMvc.perform(post("/api/ai/code-review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"language\": \"java\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.code").value("code is required"));
        verifyNoInteractions(aiReviewClient);
    }

    @Test
    void notConfiguredReturns503() throws Exception {
        when(aiReviewClient.isConfigured()).thenReturn(false);

        mockMvc.perform(post("/api/ai/code-review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"language\": \"java\", \"code\": \"class A {}\"}"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void providerFailureReturns502() throws Exception {
        when(aiReviewClient.isConfigured()).thenReturn(true);
        when(aiReviewClient.reviewCode(any(), any())).thenThrow(new AiServiceException("Could not reach the Claude API"));

        mockMvc.perform(post("/api/ai/code-review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"language\": \"java\", \"code\": \"class A {}\"}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value("Could not reach the Claude API"));
    }
}
