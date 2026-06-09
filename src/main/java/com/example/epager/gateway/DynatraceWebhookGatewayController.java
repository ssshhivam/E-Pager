package com.example.epager.gateway;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/gateway/webhooks")
public class DynatraceWebhookGatewayController {

    private final DynatraceWebhookGatewayService gatewayService;

    public DynatraceWebhookGatewayController(DynatraceWebhookGatewayService gatewayService) {
        this.gatewayService = gatewayService;
    }

    @PostMapping(value = "/dynatrace", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> receiveDynatraceProblem(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody String rawPayload
    ) {
        return gatewayService.forwardToEpager(authorization, rawPayload);
    }
}
