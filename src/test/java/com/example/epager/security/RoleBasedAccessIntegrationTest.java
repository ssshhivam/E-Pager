package com.example.epager.security;

import com.example.epager.escalation.EscalationEventRepository;
import com.example.epager.incident.Incident;
import com.example.epager.incident.IncidentRepository;
import com.example.epager.incident.IncidentStatus;
import com.example.epager.notification.NotificationDeliveryEventRepository;
import com.example.epager.notification.NotificationLogRepository;
import com.example.epager.security.dto.LoginResponse;
import com.example.epager.user.AppUser;
import com.example.epager.user.AppUserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Map;

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

    private AppUser shivam;
    private AppUser ravi;

    @BeforeEach
    void resetIncidents() {
        notificationDeliveryEventRepository.deleteAll();
        notificationLogRepository.deleteAll();
        escalationEventRepository.deleteAll();
        incidentRepository.deleteAll();

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

    private String login(String email) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", "password"
                        ))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readValue(response, LoginResponse.class).accessToken();
    }

    private String bearer(String token) {
        return "Bearer " + token;
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
