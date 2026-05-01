package com.openfinova.banking.tp.converter;

import com.openfinova.banking.common.lib.converter.ListToJsonConverter;
import com.openfinova.banking.tp.entity.CompensationStep;
import jakarta.persistence.Converter;
import tools.jackson.core.type.TypeReference;

import java.util.List;

/**
 * JPA AttributeConverter for converting List<CompensationStep> to/from JSON.
 * Used to store compensation steps as JSONB in PostgreSQL.
 */
@Converter
public class CompensationStepListConverter extends ListToJsonConverter<CompensationStep> {

    @Override
    protected TypeReference<List<CompensationStep>> getTypeReference() {
        return new TypeReference<List<CompensationStep>>() {
        };
    }
}
