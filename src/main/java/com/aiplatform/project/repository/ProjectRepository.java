package com.aiplatform.project.repository;

import com.aiplatform.project.model.Project;
import com.aiplatform.project.model.ProjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findAllByOrderByCreatedAtDesc();

    List<Project> findByStatusOrderByCreatedAtDesc(ProjectStatus status);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
