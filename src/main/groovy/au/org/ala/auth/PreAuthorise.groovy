package au.org.ala.auth

import java.lang.annotation.Documented
import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

/**
 * Annotation to check if user has a specific role.
 *
 * @author Nick dos Remedios (nick.dosremedios@csiro.au)
 */
@Target([ElementType.TYPE, ElementType.METHOD])
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PreAuthorise {
    String requiredRole() default "ROLE_ADMIN"
    String redirectController() default "userdetails"
    String redirectAction() default "index"
    String requiredScope() default "read:userdetails"
}

