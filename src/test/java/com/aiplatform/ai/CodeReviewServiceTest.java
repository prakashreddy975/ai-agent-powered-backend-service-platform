package com.aiplatform.ai;

import com.aiplatform.ai.client.AiReviewClient;
import com.aiplatform.ai.dto.CodeReviewRequest;
import com.aiplatform.ai.dto.CodeReviewResponse;
import com.aiplatform.ai.dto.IssueSeverity;
import com.aiplatform.ai.dto.ReviewIssue;
import com.aiplatform.ai.service.CodeReviewService;
import com.aiplatform.common.exception.AiNotConfiguredException;
import com.aiplatform.common.exception.AiServiceException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CodeReviewServiceTest {

    @Mock
    private AiReviewClient aiReviewClient;

    @InjectMocks
    private CodeReviewService codeReviewService;

    @Test
    void returnsReviewFromClientAndNormalizesLanguage() {
        CodeReviewResponse review = new CodeReviewResponse(
                "Readable but has a null-safety bug.",
                List.of(new ReviewIssue(IssueSeverity.HIGH, "line 3", "Possible NPE", "Check for null")),
                List.of("Add unit tests"));
        when(aiReviewClient.isConfigured()).thenReturn(true);
        when(aiReviewClient.reviewCode("java", "String s = null; s.length();")).thenReturn(review);

        CodeReviewResponse result = codeReviewService.review(new CodeReviewRequest(" Java ", "String s = null; s.length();"));

        assertThat(result).isEqualTo(review);
        assertThat(result.issues()).hasSize(1);
    }

    @Test
    void throwsWhenClientNotConfigured() {
        when(aiReviewClient.isConfigured()).thenReturn(false);

        assertThatThrownBy(() -> codeReviewService.review(new CodeReviewRequest("java", "class A {}")))
                .isInstanceOf(AiNotConfiguredException.class)
                .hasMessageContaining("ANTHROPIC_API_KEY");
        verify(aiReviewClient, never()).reviewCode(any(), any());
    }

    @Test
    void rejectsEmptyReviewFromProvider() {
        when(aiReviewClient.isConfigured()).thenReturn(true);
        when(aiReviewClient.reviewCode(any(), any())).thenReturn(new CodeReviewResponse(" ", null, null));

        assertThatThrownBy(() -> codeReviewService.review(new CodeReviewRequest("java", "class A {}")))
                .isInstanceOf(AiServiceException.class);
    }

    @Test
    void propagatesProviderFailure() {
        when(aiReviewClient.isConfigured()).thenReturn(true);
        when(aiReviewClient.reviewCode(any(), any())).thenThrow(new AiServiceException("Claude API returned an error (HTTP 529)"));

        assertThatThrownBy(() -> codeReviewService.review(new CodeReviewRequest("java", "class A {}")))
                .isInstanceOf(AiServiceException.class)
                .hasMessageContaining("529");
    }

    @Test
    void nullListsAreNormalizedToEmpty() {
        CodeReviewResponse response = new CodeReviewResponse("ok", null, null);
        assertThat(response.issues()).isEmpty();
        assertThat(response.recommendations()).isEmpty();
    }
}
