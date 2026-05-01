package com.openfinova.banking.common.lib.converter;

import java.util.Collection;

import jakarta.persistence.AttributeConverter;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Base for JPA converters that serialize a collection to JSON.
 * Subclasses provide the collection type via {@link #getTypeReference()} and
 * the empty instance via {@link #emptyCollection()}.
 */
abstract class AbstractCollectionToJsonConverter<C extends Collection<T>, T> implements AttributeConverter<C, String> {

    final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

    protected abstract TypeReference<C> getTypeReference();

    protected abstract C emptyCollection();

    @Override
    public String convertToDatabaseColumn(C attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JacksonException e) {
            throw new IllegalArgumentException("Error converting collection to JSON", e);
        }
    }

    @Override
    public C convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return emptyCollection();
        }

        try {
            return objectMapper.readValue(dbData, getTypeReference());
        } catch (JacksonException e) {
            throw new IllegalArgumentException("Error converting JSON to collection", e);
        }
    }
}
