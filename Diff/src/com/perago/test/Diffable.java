package com.perago.test;

import java.lang.annotation.*;

/**
 * Indicates that this class is {@link com.perago.test.DiffEngine#calculate(String, Object, Object) DiffEngine.calculate()}-aware.
 * <p/>
 * A class that uses this annotation should use {@link DiffField} to specify
 * which fields are to be included when calculating the difference between objects
 * of this type.
 *
 * @see DiffField
 * @see com.perago.test.DiffEngine#calculate(String, Object, Object)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface Diffable {
}
