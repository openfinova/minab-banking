package com.openfinova.banking.identity.converter;

import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.LinkedHashMap;
import java.util.Map;
import com.openfinova.banking.identity.audit.AuditEventDetail;

/**
 * JPA {@link AttributeConverter} that serialises an {@link AuditEventDetail} value object to a
 * JSON string for storage in the {@code details_json TEXT} column and deserialises it back on
 * read.
 *
 * <p>A static {@link ObjectMapper} instance is used here because {@code AttributeConverter}
 * implementations run outside the Spring container lifecycle during entity materialisation.
 * {@link ObjectMapper} is thread-safe for concurrent reads/writes.
 */
@Converter
public class AuditEventDetailConverter implements AttributeConverter<AuditEventDetail, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<Map<String, Object>>() {
    };

    @Override
    public String convertToDatabaseColumn(AuditEventDetail detail) {
        if (detail == null) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(detail.getFields());
        } catch (JacksonException e) {
            throw new IllegalStateException("Failed to serialise AuditEventDetail to JSON", e);
        }
    }

    @Override
    public AuditEventDetail convertToEntityAttribute(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            Map<String, Object> fields = MAPPER.readValue(json, MAP_TYPE);
            Map<String, Object> copy = new LinkedHashMap<>(fields);
            return AuditEventDetail.fromRawFields(copy);
        } catch (JacksonException e) {
            throw new IllegalStateException("Failed to deserialise AuditEventDetail from JSON: " + json, e);
        }
    }
}
