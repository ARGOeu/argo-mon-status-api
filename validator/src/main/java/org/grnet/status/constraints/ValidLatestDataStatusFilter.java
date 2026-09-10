package org.grnet.status.constraints;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import org.grnet.status.validators.LatestDataStatusFilterValidator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@RegisterForReflection
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = LatestDataStatusFilterValidator.class)
public @interface ValidLatestDataStatusFilter {

    String message() default "Invalid error filter";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
