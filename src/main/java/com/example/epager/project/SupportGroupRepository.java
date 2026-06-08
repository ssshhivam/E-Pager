package com.example.epager.project;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SupportGroupRepository extends JpaRepository<SupportGroup, Long> {

    List<SupportGroup> findByProjectAndActiveTrue(Project project);

    Optional<SupportGroup> findByProjectAndGroupKeyIgnoreCaseAndActiveTrue(Project project, String groupKey);
}
