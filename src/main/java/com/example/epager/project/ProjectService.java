package com.example.epager.project;

import com.example.epager.project.dto.AddGroupMemberRequest;
import com.example.epager.project.dto.ProjectRequest;
import com.example.epager.project.dto.SupportGroupRequest;
import com.example.epager.user.AppUser;
import com.example.epager.user.AppUserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final SupportGroupRepository supportGroupRepository;
    private final SupportGroupMemberRepository supportGroupMemberRepository;
    private final AppUserRepository appUserRepository;

    public ProjectService(
            ProjectRepository projectRepository,
            SupportGroupRepository supportGroupRepository,
            SupportGroupMemberRepository supportGroupMemberRepository,
            AppUserRepository appUserRepository
    ) {
        this.projectRepository = projectRepository;
        this.supportGroupRepository = supportGroupRepository;
        this.supportGroupMemberRepository = supportGroupMemberRepository;
        this.appUserRepository = appUserRepository;
    }

    @Transactional(readOnly = true)
    public List<Project> findAllProjects() {
        return projectRepository.findAll();
    }

    @Transactional
    public Project createProject(ProjectRequest request) {
        Project project = new Project();
        project.setProjectKey(request.projectKey());
        project.setName(request.name());
        project.setDescription(request.description());
        project.setActive(request.active() == null || request.active());
        project.setCreatedAt(LocalDateTime.now());
        return projectRepository.save(project);
    }

    @Transactional(readOnly = true)
    public List<SupportGroup> findGroups(Long projectId) {
        return supportGroupRepository.findByProjectAndActiveTrue(findProject(projectId));
    }

    @Transactional
    public SupportGroup createGroup(Long projectId, SupportGroupRequest request) {
        SupportGroup group = new SupportGroup();
        group.setProject(findProject(projectId));
        group.setGroupKey(request.groupKey());
        group.setName(request.name());
        group.setActive(request.active() == null || request.active());
        group.setCreatedAt(LocalDateTime.now());
        return supportGroupRepository.save(group);
    }

    @Transactional(readOnly = true)
    public List<SupportGroupMember> findMembers(Long groupId) {
        return supportGroupMemberRepository.findBySupportGroupAndActiveTrue(findGroup(groupId));
    }

    @Transactional
    public SupportGroupMember addMember(Long groupId, AddGroupMemberRequest request) {
        SupportGroup group = findGroup(groupId);
        AppUser user = appUserRepository.findById(request.userId())
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + request.userId()));

        return supportGroupMemberRepository.findBySupportGroupAndUserAndActiveTrue(group, user)
                .orElseGet(() -> {
                    SupportGroupMember member = new SupportGroupMember();
                    member.setSupportGroup(group);
                    member.setUser(user);
                    member.setActive(true);
                    member.setCreatedAt(LocalDateTime.now());
                    return supportGroupMemberRepository.save(member);
                });
    }

    private Project findProject(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Project not found: " + projectId));
    }

    private SupportGroup findGroup(Long groupId) {
        return supportGroupRepository.findById(groupId)
                .orElseThrow(() -> new EntityNotFoundException("Support group not found: " + groupId));
    }
}
