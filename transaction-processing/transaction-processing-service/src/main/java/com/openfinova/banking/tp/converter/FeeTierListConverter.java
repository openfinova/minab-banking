package com.openfinova.banking.tp.converter;

import com.openfinova.banking.common.lib.converter.ListToJsonConverter;
import com.openfinova.banking.tp.api.entity.FeeTier;
import jakarta.persistence.Converter;
import tools.jackson.core.type.TypeReference;

import java.util.List;

/**
 * JPA AttributeConverter for converting List<FeeTier> to/from JSON.
 * Used to store fee tier configurations as JSONB in PostgreSQL.
 */
@Converter
public class FeeTierListConverter extends ListToJsonConverter<FeeTier> {

    @Override
    protected TypeReference<List<FeeTier>> getTypeReference() {
        return new TypeReference<List<FeeTier>>() {
        };
    }
}
