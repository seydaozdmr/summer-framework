package io.summerframework.core.web.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface RequestParam {

    String NO_DEFAULT_VALUE = "\n\t\n\t\n\t";

    String value() default "";

    boolean required() default true;

    String defaultValue() default NO_DEFAULT_VALUE;
}
