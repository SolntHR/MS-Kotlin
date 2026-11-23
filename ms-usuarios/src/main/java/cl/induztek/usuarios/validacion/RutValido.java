package cl.induztek.usuarios.validacion;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = RutValidoValidator.class)
public @interface RutValido {

    String message() default "RUT inválido: revisa el dígito verificador";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
