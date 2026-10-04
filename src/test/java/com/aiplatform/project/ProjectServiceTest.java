package com.aiplatform.project;

import com.aiplatform.common.exception.DuplicateResourceException;
import com.aiplatform.common.exception.ResourceNotFoundException;
import com.aiplatform.project.dto.ProjectRequest;
import com.aiplatform.project.dto.ProjectResponse;
import com.aiplatform.project.model.Project;
import com.aiplatform.project.model.ProjectStatus;
import com.aiplatform.project.repository.ProjectRepository;
import com.aiplatform.project.service.ProjectService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private ProjectService projectService;

    @Test
    void createDefaultsStatusToPlannedAndTrimsName() {
        when(projectRepository.existsByNameIgnoreCase("Payments API")).thenReturn(false);
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProjectResponse response = projectService.create(new ProjectRequest("  Payments API ", "Billing service", null));

        ArgumentCaptor<Project> saved = ArgumentCaptor.forClass(Project.class);
        verify(projectRepository).save(saved.capture());
        assertThat(saved.getValue().getName()).isEqualTo("Payments API");
        assertThat(response.status()).isEqualTo(ProjectStatus.PLANNED);
        assertThat(response.description()).isEqualTo("Billing service");
    }

    @Test
    void createRejectsDuplicateName() {
        when(projectRepository.existsByNameIgnoreCase("Payments API")).thenReturn(true);

        assertThatThrownBy(() -> projectService.create(new ProjectRequest("Payments API", null, null)))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Payments API");
        verify(projectRepository, never()).save(any());
    }

    @Test
    void findAllFiltersByStatusWhenProvided() {
        when(projectRepository.findByStatusOrderByCreatedAtDesc(ProjectStatus.ACTIVE))
                .thenReturn(List.of(new Project("A", null, ProjectStatus.ACTIVE)));

        List<ProjectResponse> result = projectService.findAll(ProjectStatus.ACTIVE);

        assertThat(result).extracting(ProjectResponse::name).containsExactly("A");
        verify(projectRepository, never()).findAllByOrderByCreatedAtDesc();
    }

    @Test
    void findByIdThrowsWhenMissing() {
        when(projectRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.findById(42L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("42");
    }

    @Test
    void updateChangesFieldsAndKeepsStatusWhenNotProvided() {
        Project existing = new Project("Old", "old", ProjectStatus.ACTIVE);
        when(projectRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(projectRepository.existsByNameIgnoreCaseAndIdNot("New", 1L)).thenReturn(false);
        when(projectRepository.saveAndFlush(existing)).thenReturn(existing);

        ProjectResponse response = projectService.update(1L, new ProjectRequest("New", "new", null));

        assertThat(response.name()).isEqualTo("New");
        assertThat(response.description()).isEqualTo("new");
        assertThat(response.status()).isEqualTo(ProjectStatus.ACTIVE);
    }

    @Test
    void updateRejectsNameUsedByAnotherProject() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(new Project("Old", null, ProjectStatus.PLANNED)));
        when(projectRepository.existsByNameIgnoreCaseAndIdNot("Taken", 1L)).thenReturn(true);

        assertThatThrownBy(() -> projectService.update(1L, new ProjectRequest("Taken", null, null)))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void deleteRemovesExistingProject() {
        Project existing = new Project("A", null, ProjectStatus.PLANNED);
        when(projectRepository.findById(5L)).thenReturn(Optional.of(existing));

        projectService.delete(5L);

        verify(projectRepository).delete(existing);
    }

    @Test
    void deleteThrowsWhenMissing() {
        when(projectRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.delete(5L)).isInstanceOf(ResourceNotFoundException.class);
        verify(projectRepository, never()).delete(any());
    }
}
