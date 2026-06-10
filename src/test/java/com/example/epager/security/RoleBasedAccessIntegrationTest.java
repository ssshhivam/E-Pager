package com.example.epager.security;

import com.example.epager.escalation.EscalationEventRepository;
import com.example.epager.incident.Incident;
import com.example.epager.incident.IncidentRepository;
import com.example.epager.incident.IncidentStatus;
import com.example.epager.notification.NotificationDeliveryEventRepository;
import com.example.epager.notification.NotificationLogRepository;
import com.example.epager.security.dto.LoginResponse;
import com.example.epager.user.AppRole;
import com.example.epager.user.AppUser;
import com.example.epager.user.AppUserRepository;
import com.example.epager.webhook.WebhookAuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=${EPAGER_TEST_DB_URL:jdbc:postgresql://localhost:5432/epager_test}",
        "spring.datasource.username=${EPAGER_TEST_DB_USERNAME:epager}",
        "spring.datasource.password=${EPAGER_TEST_DB_PASSWORD:epager}",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.clean-disabled=false"
})
@AutoConfigureMockMvc
class RoleBasedAccessIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private EscalationEventRepository escalationEventRepository;

    @Autowired
    private NotificationDeliveryEventRepository notificationDeliveryEventRepository;

    @Autowired
    private NotificationLogRepository notificationLogRepository;

    @Autowired
    private WebhookAuditLogRepository webhookAuditLogRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private AppUser shivam;
    private AppUser ravi;

    @BeforeEach
    void resetIncidents() {
        refreshTokenRepository.deleteAll();
        notificationDeliveryEventRepository.deleteAll();
        notificationLogRepository.deleteAll();
        escalationEventRepository.deleteAll();
        incidentRepository.deleteAll();
        webhookAuditLogRepository.deleteAll();

        shivam = appUserRepository.findByEmailIgnoreCase("shivam.engineer@example.com")
                .orElseThrow();
        ravi = appUserRepository.findByEmailIgnoreCase("ravi.lead@example.com")
                .orElseThrow();
    }

    @Test
    void adminCanManageAdminResources() throws Exception {
        String adminToken = login("admin@epager.local");

        mockMvc.perform(get("/api/users").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/projects").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/webhooks/sources").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/escalation-policies").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk());
    }

    @Test
    void managerCanManagePoliciesAndIncidentsButNotAdminResources() throws Exception {
        String managerToken = login("ravi.lead@example.com");

        mockMvc.perform(get("/api/escalation-policies").header("Authorization", bearer(managerToken)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/incidents").header("Authorization", bearer(managerToken)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/users").header("Authorization", bearer(managerToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/projects").header("Authorization", bearer(managerToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/webhooks/sources").header("Authorization", bearer(managerToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void engineerCanOnlyWorkWithAssignedIncidents() throws Exception {
        Incident assignedToShivam = incidentRepository.save(incident("assigned-to-shivam", shivam));
        Incident assignedToRavi = incidentRepository.save(incident("assigned-to-ravi", ravi));
        String engineerToken = login("shivam.engineer@example.com");

        mockMvc.perform(get("/api/incidents").header("Authorization", bearer(engineerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(assignedToShivam.getId()));

        mockMvc.perform(get("/api/incidents/{incidentId}", assignedToRavi.getId())
                        .header("Authorization", bearer(engineerToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/incidents/{incidentId}/acknowledge", assignedToShivam.getId())
                        .header("Authorization", bearer(engineerToken))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACKNOWLEDGED"));

        mockMvc.perform(post("/api/incidents/{incidentId}/resolve", assignedToShivam.getId())
                        .header("Authorization", bearer(engineerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));

        mockMvc.perform(post("/api/incidents/{incidentId}/acknowledge", assignedToRavi.getId())
                        .header("Authorization", bearer(engineerToken))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void incidentListReturnsNewestFirst() throws Exception {
        Incident olderIncident = incident("older-incident", shivam);
        olderIncident.setCreatedAt(LocalDateTime.now().minusMinutes(10));
        olderIncident = incidentRepository.save(olderIncident);

        Incident newerIncident = incident("newer-incident", shivam);
        newerIncident.setCreatedAt(LocalDateTime.now());
        newerIncident = incidentRepository.save(newerIncident);

        String adminToken = login("admin@epager.local");
        String engineerToken = login("shivam.engineer@example.com");

        mockMvc.perform(get("/api/incidents").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(newerIncident.getId()))
                .andExpect(jsonPath("$[1].id").value(olderIncident.getId()));

        mockMvc.perform(get("/api/incidents").header("Authorization", bearer(engineerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(newerIncident.getId()))
                .andExpect(jsonPath("$[1].id").value(olderIncident.getId()));
    }

    @Test
    void unauthenticatedUsersCannotAccessProtectedApis() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/incidents"))
                .andExpect(status().isForbidden());
    }

    @Test
    void dynatraceGatewayRejectsMissingGatewayToken() throws Exception {
        mockMvc.perform(post("/gateway/webhooks/dynatrace")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void simulatedDynatraceCriticalAlertCreatesIncidentAndNotification() throws Exception {
        String adminToken = login("admin@epager.local");

        String response = mockMvc.perform(post("/api/testing/alerts/dynatrace/critical")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.source").value("dynatrace"))
                .andExpect(jsonPath("$.severity").value("critical"))
                .andExpect(jsonPath("$.payload.problemTitle").value("Payments service failure rate is critical"))
                .andExpect(jsonPath("$.incident.status").value("TRIGGERED"))
                .andExpect(jsonPath("$.incident.assignedUserName").value("Shivam Engineer"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long incidentId = objectMapper.readTree(response).path("incident").path("id").asLong();
        assertTrue(notificationLogRepository.findAll().stream()
                .anyMatch(log -> log.getIncident() != null && incidentId.equals(log.getIncident().getId())));
    }

    @Test
    void rejectedWebhookRequestCreatesAuditLog() throws Exception {
        mockMvc.perform(post("/api/alerts/grafana")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());

        assertTrue(webhookAuditLogRepository.findAll().stream()
                .anyMatch(log -> !log.isAccepted()
                        && "grafana".equals(log.getSourceName())
                        && "Missing HMAC timestamp".equals(log.getRejectionReason())));
    }

    @Test
    void adminCannotCreateUserWithoutPassword() throws Exception {
        String adminToken = login("admin@epager.local");

        mockMvc.perform(post("/api/users")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "No Password User",
                                "email", "no.password." + System.nanoTime() + "@example.com",
                                "role", "ENGINEER"
                        ))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void refreshTokenRotatesAndRejectsOldRefreshToken() throws Exception {
        LoginResponse login = loginResponse("admin@epager.local", "password");

        String refreshedResponse = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "refreshToken", login.refreshToken()
                        ))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        LoginResponse refreshed = objectMapper.readValue(refreshedResponse, LoginResponse.class);
        assertNotNull(refreshed.accessToken());
        assertNotNull(refreshed.refreshToken());
        assertNotEquals(login.refreshToken(), refreshed.refreshToken());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "refreshToken", login.refreshToken()
                        ))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void changePasswordRevokesRefreshTokensAndAllowsNewPassword() throws Exception {
        AppUser user = createTestUser("oldPassword1");
        LoginResponse login = loginResponse(user.getEmail(), "oldPassword1");

        mockMvc.perform(post("/api/auth/change-password")
                        .header("Authorization", bearer(login.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "currentPassword", "oldPassword1",
                                "newPassword", "newPassword1"
                        ))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "refreshToken", login.refreshToken()
                        ))))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", user.getEmail(),
                                "password", "oldPassword1"
                        ))))
                .andExpect(status().isForbidden());

        loginResponse(user.getEmail(), "newPassword1");
    }

    private String login(String email) throws Exception {
        return loginResponse(email, "password").accessToken();
    }

    private LoginResponse loginResponse(String email, String password) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", password
                        ))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readValue(response, LoginResponse.class);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private AppUser createTestUser(String password) {
        AppUser user = new AppUser();
        user.setName("Password Test User");
        user.setEmail("password.test." + System.nanoTime() + "@example.com");
        user.setPhoneNumber("+19999999999");
        user.setRole(AppRole.ENGINEER);
        user.setPasswordHash(passwordEncoder.encode(password));
        return appUserRepository.save(user);
    }

    private Incident incident(String externalAlertId, AppUser assignedUser) {
        Incident incident = new Incident();
        incident.setExternalAlertId(externalAlertId);
        incident.setSource("TEST");
        incident.setProjectKey("payments");
        incident.setGroupKey("primary-support");
        incident.setServiceName("payments");
        incident.setSeverity("critical");
        incident.setTitle("Role access test");
        incident.setDescription("Created by role-based access integration test");
        incident.setStatus(IncidentStatus.TRIGGERED);
        incident.setCurrentEscalationLevel(1);
        incident.setAssignedUser(assignedUser);
        incident.setCreatedAt(LocalDateTime.now());
        return incident;
    }
}
