package com.openfinova.banking.common.lib.converter;

import java.util.ArrayList;
import java.util.List;

import tools.jackson.core.type.TypeReference;

/**
 * Generic JPA AttributeConverter base for converting List to/from JSON.
 * Subclass and provide getTypeReference() for the concrete element type.
 *
 * Example:
 *
 *   @Converter
 *   public class FeeTierListConverter extends ListToJsonConverter<FeeTier> {
 *       @Override
 *       protected TypeReference<List<FeeTier>> getTypeReference() {
 *           return new TypeReference<List<FeeTier>>() {};
 *       }
 *   }
 */
public abstract class ListToJsonConverter<T> extends AbstractCollectionToJsonConverter<List<T>, T> {

    @Override
    protected abstract TypeReference<List<T>> getTypeReference();

    @Override
    protected List<T> emptyCollection() {
        return new ArrayList<>();
    }
}
