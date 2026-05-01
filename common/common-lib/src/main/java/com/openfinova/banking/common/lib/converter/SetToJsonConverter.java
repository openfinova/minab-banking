package com.openfinova.banking.common.lib.converter;

import java.util.HashSet;
import java.util.Set;

import tools.jackson.core.type.TypeReference;

/**
 * Generic JPA AttributeConverter base for converting Set to/from JSON.
 * Subclass and provide getTypeReference() for the concrete element type.
 *
 * Example:
 *
 *   @Converter
 *   public class AccountPermissionSetConverter extends SetToJsonConverter<AccountPermission> {
 *       @Override
 *       protected TypeReference<Set<AccountPermission>> getTypeReference() {
 *           return new TypeReference<Set<AccountPermission>>() {};
 *       }
 *   }
 */
public abstract class SetToJsonConverter<T> extends AbstractCollectionToJsonConverter<Set<T>, T> {

    @Override
    protected abstract TypeReference<Set<T>> getTypeReference();

    @Override
    protected Set<T> emptyCollection() {
        return new HashSet<>();
    }
}
