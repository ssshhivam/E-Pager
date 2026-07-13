package com.example.epager.security;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import com.example.epager.escalation.EscalationEventRepository;
import com.example.epager.incident.Incident;
import com.example.epager.incident.IncidentRepository;
import com.example.epager.incident.IncidentStatus;
import com.example.epager.notification.NotificationDeliveryEventRepository;
import com.example.epager.notification.NotificationLog;
import com.example.epager.notification.NotificationLogRepository;
import com.example.epager.security.dto.LoginResponse;
import com.example.epager.user.AppRole;
import com.example.epager.user.AppUser;
import com.example.epager.user.AppUserRepository;
import com.example.epager.user.roster.AppUserRoster;
import com.example.epager.user.roster.AppUserRosterRepository;
import com.example.epager.user.roster.Shift;
import com.example.epager.user.roster.ShiftRepository;
import com.example.epager.webhook.WebhookAuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

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
    private ShiftRepository shiftRepository;
    
    @Autowired
    private AppUserRosterRepository rosterRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;

    private AppUser shivam;
    private AppUser ravi;
    private AppUser manish;

    @BeforeEach
    void resetIncidents() {
        refreshTokenRepository.deleteAll();
        notificationDeliveryEventRepository.deleteAll();
        notificationLogRepository.deleteAll();
        escalationEventRepository.deleteAll();
        incidentRepository.deleteAll();
        webhookAuditLogRepository.deleteAll();
        ensureShifts();
       

        shivam = appUserRepository.findByEmailIgnoreCase("shivam.engineer@example.com")
                .orElseThrow();
        ravi = appUserRepository.findByEmailIgnoreCase("ravi.lead@example.com")
                .orElseThrow();
        manish = appUserRepository.findByEmailIgnoreCase("manish.manager@example.com")
                .orElseThrow();
        ensureRoster(shivam, ravi, manish);
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
    void dashboardShowsSummaryAndRespectsEngineerVisibility() throws Exception {
        Incident assignedToShivam = incidentRepository.save(incident("dashboard-shivam", shivam));
        incidentRepository.save(incident("dashboard-ravi", ravi));

        String adminToken = login("admin@epager.local");
        String engineerToken = login("shivam.engineer@example.com");

        mockMvc.perform(get("/api/dashboard").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incidents.total").value(2))
                .andExpect(jsonPath("$.incidents.triggered").value(2))
                .andExpect(jsonPath("$.recentIncidents", hasSize(2)));

        mockMvc.perform(get("/api/dashboard").header("Authorization", bearer(engineerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incidents.total").value(1))
                .andExpect(jsonPath("$.recentIncidents", hasSize(1)))
                .andExpect(jsonPath("$.recentIncidents[0].id").value(assignedToShivam.getId()));
    }

    @Test
    void unauthenticatedUsersCannotAccessProtectedApis() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/incidents"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/dashboard"))
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
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long incidentId = objectMapper.readTree(response).path("incident").path("id").asLong();
        List<NotificationLog> all = notificationLogRepository.findAll();
        assertTrue(all.stream()
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
    
	private void ensureShifts() {

		createShiftIfNotExists("Morning", LocalTime.of(8, 0), LocalTime.of(16, 0));

		createShiftIfNotExists("Evening", LocalTime.of(16, 0), LocalTime.MIDNIGHT);

		createShiftIfNotExists("Night", LocalTime.MIDNIGHT, LocalTime.of(8, 0));
	}

	private void createShiftIfNotExists(String shiftName, LocalTime startTime, LocalTime endTime) {
		if (shiftRepository.findByShiftNameIgnoreCase(shiftName).isPresent()) {
			return;
		}
		Shift shift = new Shift();
		shift.setShiftName(shiftName);
		shift.setStartTime(startTime);
		shift.setEndTime(endTime);
		shift.setActive(true);
		shift.setCreatedOn(LocalDateTime.now());
		shift.setUpdatedOn(LocalDateTime.now());

		shiftRepository.save(shift);
	}
	
	private void createRosterIfNotExists(AppUser user, Shift shift, LocalDate rosterDate) {
		if (rosterRepository.existsByUserIdAndShiftIdAndRosterDate(user.getId(), shift.getId(), rosterDate)) {
			return;
		}
		AppUserRoster roster = new AppUserRoster();

		roster.setUser(user);
		roster.setShift(shift);
		roster.setRosterDate(rosterDate);
		roster.setActive(true);
		roster.setCreatedOn(LocalDateTime.now());
		roster.setUpdatedOn(LocalDateTime.now());
		rosterRepository.save(roster);
	}
	
	private void ensureRoster(AppUser shivam, AppUser ravi, AppUser manish) {
		LocalDate today = LocalDate.now();
		Shift morning = shiftRepository.findByShiftNameIgnoreCase("Morning")
				.orElseThrow(() -> new RuntimeException("Morning shift not found"));
		Shift evening = shiftRepository.findByShiftNameIgnoreCase("Evening")
				.orElseThrow(() -> new RuntimeException("Evening shift not found"));
		Shift night = shiftRepository.findByShiftNameIgnoreCase("Night")
				.orElseThrow(() -> new RuntimeException("Night shift not found"));
		createRosterIfNotExists(shivam, morning, today);
		createRosterIfNotExists(ravi, evening, today);
		createRosterIfNotExists(shivam, evening, today);
		createRosterIfNotExists(manish, night, today);
	}
}
