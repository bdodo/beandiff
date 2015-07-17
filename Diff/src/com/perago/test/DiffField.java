package com.perago.test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the field to which this annotation applies should be taken into
 * consideration by {@link DiffEngine#calculate(String, Object, Object) DiffEngine.calculate()}.
 * <p/>
 * The annotation can take as a parameter the user-defined, application-specific data type
 * of the field, used by <code>DiffEngine.calculate()</code> to determine which
 * {@link DataResolver} to use.
 * <p/>
 * Note that this annotation has no meaning if used in a class that is not annotated
 * with {@link Diffable}.
 *
 * @see Diffable
 * @see DiffEngine#calculate(String, Object, Object)
 * @see DataResolver
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface DiffField {
    String value() default "";
}
