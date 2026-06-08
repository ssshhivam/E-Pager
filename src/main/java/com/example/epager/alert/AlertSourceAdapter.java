package com.example.epager.alert;

import com.example.epager.alert.dto.UnifiedAlert;
import com.fasterxml.jackson.databind.JsonNode;

public interface AlertSourceAdapter {

    String source();

    UnifiedAlert toUnifiedAlert(JsonNode payload);
}
