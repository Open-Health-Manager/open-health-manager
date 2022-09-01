package org.mitre.healthmanager.service.dto;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

@Documented
@Constraint(validatedBy = EmailLoginConstraintValidator.class)
@Target({ TYPE })
@Retention(RUNTIME)
public @interface ValidEmailLogin {

    String message() default "Invalid login";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
