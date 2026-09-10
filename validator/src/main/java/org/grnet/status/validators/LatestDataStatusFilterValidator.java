package org.grnet.status.validators;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.grnet.status.constraints.ValidLatestDataStatusFilter;
import org.grnet.status.enums.LatestDataStatusFilter;

import java.util.Arrays;
@RegisterForReflection
public class LatestDataStatusFilterValidator
        implements ConstraintValidator<ValidLatestDataStatusFilter, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }

        return Arrays.stream(LatestDataStatusFilter.values())
                .anyMatch(e -> e.getValue().equalsIgnoreCase(value));
    }
}
