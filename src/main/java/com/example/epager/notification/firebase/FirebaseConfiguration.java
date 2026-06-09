package com.example.epager.notification.firebase;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.io.FileInputStream;
import java.io.IOException;

@Configuration
@ConditionalOnProperty(prefix = "epager.firebase", name = "enabled", havingValue = "true")
public class FirebaseConfiguration {

    private static final String APP_NAME = "epager";

    @Bean
    public FirebaseApp epagerFirebaseApp(
            @Value("${epager.firebase.service-account-path}") String serviceAccountPath,
            @Value("${epager.firebase.project-id:}") String projectId
    ) throws IOException {
        if (!StringUtils.hasText(serviceAccountPath)) {
            throw new IllegalStateException("epager.firebase.service-account-path is required when Firebase is enabled");
        }

        for (FirebaseApp app : FirebaseApp.getApps()) {
            if (APP_NAME.equals(app.getName())) {
                return app;
            }
        }

        try (FileInputStream serviceAccount = new FileInputStream(serviceAccountPath)) {
            FirebaseOptions.Builder options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount));
            if (StringUtils.hasText(projectId)) {
                options.setProjectId(projectId);
            }
            return FirebaseApp.initializeApp(options.build(), APP_NAME);
        }
    }

    @Bean
    public FirebaseMessaging firebaseMessaging(FirebaseApp epagerFirebaseApp) {
        return FirebaseMessaging.getInstance(epagerFirebaseApp);
    }
}
