package com.example.epager.project;

import com.example.epager.project.dto.AddGroupMemberRequest;
import com.example.epager.project.dto.ProjectRequest;
import com.example.epager.project.dto.ProjectResponse;
import com.example.epager.project.dto.SupportGroupMemberResponse;
import com.example.epager.project.dto.SupportGroupRequest;
import com.example.epager.project.dto.SupportGroupResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping
    public List<ProjectResponse> listProjects() {
        return projectService.findAllProjects().stream()
                .map(ProjectResponse::from)
                .toList();
    }

    @PostMapping
    public ProjectResponse createProject(@Valid @RequestBody ProjectRequest request) {
        return ProjectResponse.from(projectService.createProject(request));
    }

    @GetMapping("/{projectId}/groups")
    public List<SupportGroupResponse> listGroups(@PathVariable Long projectId) {
        return projectService.findGroups(projectId).stream()
                .map(SupportGroupResponse::from)
                .toList();
    }

    @PostMapping("/{projectId}/groups")
    public SupportGroupResponse createGroup(
            @PathVariable Long projectId,
            @Valid @RequestBody SupportGroupRequest request
    ) {
        return SupportGroupResponse.from(projectService.createGroup(projectId, request));
    }

    @GetMapping("/groups/{groupId}/members")
    public List<SupportGroupMemberResponse> listMembers(@PathVariable Long groupId) {
        return projectService.findMembers(groupId).stream()
                .map(SupportGroupMemberResponse::from)
                .toList();
    }

    @PostMapping("/groups/{groupId}/members")
    public SupportGroupMemberResponse addMember(
            @PathVariable Long groupId,
            @Valid @RequestBody AddGroupMemberRequest request
    ) {
        return SupportGroupMemberResponse.from(projectService.addMember(groupId, request));
    }
}
