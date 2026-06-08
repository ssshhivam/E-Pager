package com.example.epager.alert;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class AlertSourceAdapterRegistry {

    private final Map<String, AlertSourceAdapter> adapters;

    public AlertSourceAdapterRegistry(List<AlertSourceAdapter> adapters) {
        this.adapters = adapters.stream()
                .collect(Collectors.toMap(adapter -> key(adapter.source()), Function.identity()));
    }

    public AlertSourceAdapter getAdapter(String source) {
        AlertSourceAdapter adapter = adapters.get(key(source));
        if (adapter == null) {
            throw new EntityNotFoundException("Unsupported alert source: " + source);
        }
        return adapter;
    }

    private String key(String source) {
        return source == null ? "" : source.toLowerCase(Locale.ROOT);
    }
}
