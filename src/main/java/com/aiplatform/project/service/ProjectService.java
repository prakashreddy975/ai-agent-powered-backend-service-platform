package com.aiplatform.project.service;

import com.aiplatform.common.exception.DuplicateResourceException;
import com.aiplatform.common.exception.ResourceNotFoundException;
import com.aiplatform.project.dto.ProjectRequest;
import com.aiplatform.project.dto.ProjectResponse;
import com.aiplatform.project.model.Project;
import com.aiplatform.project.model.ProjectStatus;
import com.aiplatform.project.repository.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ProjectService {

    private final ProjectRepository projectRepository;

    public ProjectService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    @Transactional
    public ProjectResponse create(ProjectRequest request) {
        String name = request.name().trim();
        if (projectRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateResourceException("A project named '" + name + "' already exists");
        }
        ProjectStatus status = request.status() != null ? request.status() : ProjectStatus.PLANNED;
        Project saved = projectRepository.save(new Project(name, request.description(), status));
        return ProjectResponse.from(saved);
    }

    public List<ProjectResponse> findAll(ProjectStatus status) {
        List<Project> projects = status == null
                ? projectRepository.findAllByOrderByCreatedAtDesc()
                : projectRepository.findByStatusOrderByCreatedAtDesc(status);
        return projects.stream().map(ProjectResponse::from).toList();
    }

    public ProjectResponse findById(Long id) {
        return ProjectResponse.from(getProject(id));
    }

    @Transactional
    public ProjectResponse update(Long id, ProjectRequest request) {
        Project project = getProject(id);
        String name = request.name().trim();
        if (projectRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new DuplicateResourceException("A project named '" + name + "' already exists");
        }
        project.setName(name);
        project.setDescription(request.description());
        if (request.status() != null) {
            project.setStatus(request.status());
        }
        // saveAndFlush so the @PreUpdate timestamp is reflected in the response
        return ProjectResponse.from(projectRepository.saveAndFlush(project));
    }

    @Transactional
    public void delete(Long id) {
        Project project = getProject(id);
        projectRepository.delete(project);
    }

    private Project getProject(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project with id " + id + " was not found"));
    }
}
