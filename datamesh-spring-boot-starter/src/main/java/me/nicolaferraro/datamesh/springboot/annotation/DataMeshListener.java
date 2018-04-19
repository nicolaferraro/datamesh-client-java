package me.nicolaferraro.datamesh.springboot.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataMeshListener {

    /**
     * The event name to select.
     */
    String name() default "";

    /**
     * Alias for {@link #name()}.
     */
    String value() default "";

    /**
     * The event name pattern (regular expression) to select.
     */
    String namePattern() default "";

}
