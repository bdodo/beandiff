package com.perago.test;

import java.lang.reflect.*;
import java.util.*;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author ttchiwandire@gmail.com
 */
public class DiffEngine {

    Handler ch = new ConsoleHandler();
    private static final Logger logger = Logger.getLogger(DiffEngine.class.getSimpleName());
    static int count = 0;

      public DiffEngine() {
        logger.addHandler(ch);
        logger.setLevel(Level.FINEST);
    }

    /**
     * Calculates the difference between two objects.
     * <p/>
     * This method uses the {@link Diffable} and {@link DiffField} annotations
     * to hierarchically contruct a tree-like map with the differences between
     * two objects. Specifically, the map contains an entry for each difference
     * between the two objects, where the <code>key</code> indicates where the
     * difference ocurrs, and the <code>value</code> indicates the original
     * value. The key names are constructed using a starting <code>tag</code>
     * with the field names appended.
     * <p/>
     * The <code>Diffable</code> annotation is used to tell
     * <code>DiffEngine</code>'s <code>calculate()</code> method that that class
     * is prepared for it. The <code>DiffField</code> annotation tells the
     * <code>calculate()</code> method that that field should be included when
     * calculating the difference.
     *
     * @param tag initial key name for difference map
     * @param original original object
     * @param current new object
     * @return a <code>Map&lt;String, String&gt;</code> with the differences
     * between the original and new objects, where the <code>key</code>s are the
     * fields where the differences occur, and the <code>value</code>s are the
     * original values.
     * @throws IllegalArgumentException If the two objects to compare are not of
     * the same class.
     * @see Diffable
     * @see DiffField
     */
    public Map<String, Object> calculate(String tag, Object original, Object current) {
        count++;
        if (tag == null) {
            tag = "";
        }
        final String prefix = tag.equals("") ? "" : (tag + ".");
        Map<String, Object> returnValue = new TreeMap<>();

        if (original != null && current != null && original.getClass() != current.getClass()) {
            throw new RuntimeException("'original' and 'current' arguments not of the same type"
                    + " Original:" + original.getClass().getName() + " Current:" + current.getClass().getName());
        }

        // Special case when either or both values are null is handled below
        if (original != null && current != null) {
            final Class<?> objectClass = original.getClass();
            logger.log(Level.FINER, "Diffing objects of type: {0}", objectClass.getSimpleName());
            differObjects(objectClass, original, current, returnValue, prefix, tag);
        } else if (original != current) {
            if (original == null) {
                final Class<?> objectClass = current.getClass();
                try {
                    original = objectClass.newInstance();
                } catch (InstantiationException | IllegalAccessException ex) {
                    Logger.getLogger(DiffEngine.class.getName()).log(Level.SEVERE, null, ex);
                } finally {
                    differObjects(objectClass, original, current, returnValue, prefix, tag);
                }
            } else {
                returnValue.put(tag, original == null ? "null" : original.toString());
            }
        }

        return returnValue;
    }

    private void differObjects(final Class<?> objectClass, Object original, Object current, Map<String, Object> returnValue, final String prefix, String tag) {
        // Check whether the class is Diffable.  Diffable classes are handled specially.
        if (objectClass.isAnnotationPresent(Diffable.class)) {
            logger.log(Level.FINER, "{0} is Diffable", objectClass.getSimpleName());
            for (Field field : DiffUtils.getAllFields(objectClass)) {
                // Only check fields annotated with DiffField.
                if (field.isAnnotationPresent(DiffField.class)) {
                    Object originalFieldValue;
                    Object currentFieldValue;
                    try {
                        originalFieldValue = DiffUtils.getValueForField(field, original);
                        currentFieldValue = DiffUtils.getValueForField(field, current);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        logger.log(Level.SEVERE, "Error accessing field \"{0}\" in diff. Skipping.{1}", new Object[]{field.getName(), e});
                        continue;
                    }

                    // Recursively call calculate() on the two values, appending the field name to the tag.
                    returnValue.putAll(this.calculate(prefix + field.getName(), originalFieldValue, currentFieldValue));
                }
            }
        } else {
            // For non-Diffable classes...

            logger.log(Level.FINER, "{0} is not Diffable.", objectClass.getSimpleName());
            if (!original.equals(current)) {
                List diffs = new ArrayList<String>();
                diffs.add("UPDATED | ");
                diffs.add(original == null ? "null" : original.toString());
                diffs.add(current.toString());
                //returnValue.put(tag, original.toString());
                returnValue.put(tag, diffs);
            }
        }
    }

    Map<String, Object> calculate(Person a, Person b) {
        count++;
        return calculate("", a, b);
    }

    Person apply(Person a, Person b) {
        for (Field field : DiffUtils.getAllFields(a.getClass())) {
            try {
                if (field.isAnnotationPresent(DiffField.class)) {
                    field.setAccessible(true);
                    field.set(a, DiffUtils.getValueForField(field, b));
                }
            } catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException ex) {
                Logger.getLogger(DiffEngine.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return a;
    }
}
