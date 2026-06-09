package com.example.epager.gateway;

import com.example.epager.webhook.WebhookSecurityService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;

@Service
public class DynatraceWebhookGatewayService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final RestClient restClient;
    private final String dynatraceToken;
    private final String epagerAlertUrl;
    private final String epagerHmacSecret;

    public DynatraceWebhookGatewayService(
            RestClient.Builder restClientBuilder,
            @Value("${epager.gateway.dynatrace-token}") String dynatraceToken,
            @Value("${epager.gateway.epager-alert-url}") String epagerAlertUrl,
            @Value("${epager.gateway.epager-hmac-secret}") String epagerHmacSecret
    ) {
        this.restClient = restClientBuilder.build();
        this.dynatraceToken = dynatraceToken;
        this.epagerAlertUrl = epagerAlertUrl;
        this.epagerHmacSecret = epagerHmacSecret;
    }

    public ResponseEntity<String> forwardToEpager(String authorization, String rawPayload) {
        validateAuthorization(authorization);

        String timestamp = Instant.now().toString();
        String signature = "sha256=" + hmacSha256Hex(epagerHmacSecret, timestamp + ":" + rawPayload);

        return restClient.post()
                .uri(epagerAlertUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .header(WebhookSecurityService.TIMESTAMP_HEADER, timestamp)
                .header(WebhookSecurityService.SIGNATURE_HEADER, signature)
                .body(rawPayload)
                .exchange((request, response) -> {
                    String responseBody = StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8);
                    HttpHeaders headers = new HttpHeaders();
                    response.getHeaders().forEach((name, values) -> {
                        if (HttpHeaders.CONTENT_TYPE.equalsIgnoreCase(name)) {
                            headers.put(name, values);
                        }
                    });
                    return new ResponseEntity<>(responseBody, headers, HttpStatusCode.valueOf(response.getStatusCode().value()));
                });
    }

    private void validateAuthorization(String authorization) {
        String expected = "Bearer " + dynatraceToken;
        if (authorization == null || !MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                authorization.getBytes(StandardCharsets.UTF_8)
        )) {
            throw new GatewayAuthenticationException("Invalid Dynatrace gateway token");
        }
    }

    private String hmacSha256Hex(String secret, String message) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return HexFormat.of().formatHex(mac.doFinal(message.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | InvalidKeyException exception) {
            throw new IllegalStateException("Unable to calculate gateway HMAC signature", exception);
        }
    }
}
