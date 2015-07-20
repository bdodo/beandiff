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

    private final Map<String, DataResolver> resolvers = new HashMap<>();
    
    public DiffEngine(){
        logger.addHandler(ch);
        logger.setLevel(Level.FINEST);
    }

    /**
     * Calculates the difference between two objects.
     * <p/>
     * This method uses the {@link Diffable} and {@link DiffField} annotations to
     * hierarchically contruct a tree-like map with the differences between two objects.
     * Specifically, the map contains an entry for each difference between the two objects,
     * where the <code>key</code> indicates where the difference ocurrs, and the <code>value</code>
     * indicates the original value.  The key names are constructed using a starting <code>tag</code>
     * with the field names appended.
     * <p/>
     * The <code>Diffable</code> annotation is used to tell <code>DiffEngine</code>'s <code>calculate()</code>
     * method that that class is prepared for it.  The <code>DiffField</code> annotation tells the
     * <code>calculate()</code> method that that field should be included when calculating the difference.
     *
     * @param tag      initial key name for difference map
     * @param original original object
     * @param current  new object
     * @return a <code>Map&lt;String, String&gt;</code> with the differences between the original and new objects,
     *         where the <code>key</code>s are the fields where the differences occur, and the <code>value</code>s
     *         are the original values.
     * @throws IllegalArgumentException If the two objects to compare are not of the same class.
     * @see Diffable
     * @see DiffField
     */
    public Map<String, String> calculate(String tag, Object original, Object current) {
        count++;
        if (tag == null) tag = "";
        final String prefix = tag.equals("") ? "" : (tag + ".");
        Map<String, String> returnValue = new TreeMap<>();

        if (original != null && current != null && original.getClass() != current.getClass())
            throw new RuntimeException("'original' and 'current' arguments not of the same type" +
                    " Original:" + original.getClass().getName() + " Current:" + current.getClass().getName());

        // Special case when either or both values are null is handled below
        if (original != null && current != null ) {
            final Class<?> objectClass = original.getClass();
            logger.log(Level.FINER, "Diffing objects of type: {0}", objectClass.getSimpleName());
            differObjects(objectClass, original, current, returnValue, prefix, tag);
        } else if (original != current) {
            if (original == null){
                final Class<?> objectClass = current.getClass();
                try{
                original = objectClass.newInstance();
                }
                catch (InstantiationException | IllegalAccessException ex) {
                    Logger.getLogger(DiffEngine.class.getName()).log(Level.SEVERE, null, ex);
                } finally{
                differObjects(objectClass,original, current, returnValue, prefix, tag);
                }
            }else
                returnValue.putAll(resolveObject(tag, original));
//            returnValue.put(tag, original == null ? "" : original.toString());
        }

        return returnValue;
    }

    private void differObjects(final Class<?> objectClass, Object original, Object current, Map<String, String> returnValue, final String prefix, String tag) {
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
                    
                    // Resolve the data, in case some sort of lookup or any other processing is needed.
                    DiffField annotation = field.getAnnotation(DiffField.class);
                    String dataType = annotation.value();
                    DataResolver resolver;
                    synchronized (this) {
                        resolver = resolvers.get(dataType);
                    }
                    if (resolver != null) {
                        logger.finer("Resolving data...");
                        originalFieldValue = resolver.resolve(originalFieldValue);
                        currentFieldValue = resolver.resolve(currentFieldValue);
                        logger.finer("Both data resolved.");
                    }

                    // Recursively call calculate() on the two values, appending the field name to the tag.
                    returnValue.putAll(this.calculate(prefix + field.getName(), originalFieldValue, currentFieldValue));
                }
            }
        } else {
            // For non-Diffable classes...
            
            logger.log(Level.FINER, "{0} is not Diffable.", objectClass.getSimpleName());
            // Iterate through iterable objects
            if (original instanceof Iterable) {
                logger.log(Level.FINER, "{0} is Iterable.", objectClass.getSimpleName());
                int i = 0;
                Iterator<?> oIterator = ((Iterable<?>) original).iterator();
                Iterator<?> cIterator = ((Iterable<?>) current).iterator();
                while (oIterator.hasNext() && cIterator.hasNext()) {
                    logger.log(Level.FINER, "Checking item with index: {0}", i);
                    Object oObj = oIterator.next();
                    Object cObj = cIterator.next();
                    // Recursively call calculate() on the corresponding values, appending the index.
                    returnValue.putAll(this.calculate(prefix + "idx" + ++i, oObj, cObj));
                }
                
                // If the item count is different, record it.
                if (oIterator.hasNext()) {
                    while (oIterator.hasNext()) {
                        oIterator.next();
                        i++;
                    }
                    returnValue.put(prefix + "count", Integer.toString(i));
                } else if (cIterator.hasNext()) {
                    returnValue.put(prefix + "count", Integer.toString(i));
                }
                // Iterate through map keys
            } else if (original instanceof Map) {
                Map<?, ?> oMap = (Map<?, ?>) original;
                Map<?, ?> cMap = (Map<?, ?>) current;
                for (Object key : oMap.keySet()) {
                    Object oObj = oMap.get(key);
                    Object cObj = cMap.get(key);
                    // Recursively call calculate() on the corresponding vaues, appending the key.
                    returnValue.putAll(this.calculate(prefix + key.toString(), oObj, cObj));
                }
                // If class isn't Diffable, not iterable, and not a map, simply use equals() to find any differences
            } else if (!original.equals(current)) {
                returnValue.put(tag, original.toString());
            }
        }
        // Special case when either, but not both, is null.  If both are null, there is no difference to record.
    }

    /**
     * Resolves an object using {@link Diffable Diffable} fields as appropriate.
     * <p/>
     * This method is used internally by the <code>calculate()</code> method to add the correct values
     * when the <code>current</code> object is <code>null</code> at any given point in the comparation.
     *
     * @param tag    initial key name for map
     * @param object object to be resolved
     * @return a map with all the data in the object, according to normal {@link DiffEngine DiffEngine} rules
     */
    public Map<String, String> resolveObject(String tag, Object object) {
        if (tag == null) tag = "";
        final String prefix = tag.equals("") ? "" : (tag + ".");
        Map<String, String> returnValue = new TreeMap<>();

        if (object == null)
            returnValue.put(tag, "");
        else {
            final Class<?> objectClass = object.getClass();
            logger.log(Level.FINER, "Resolving object of type: {0}", objectClass.getSimpleName());
            // Check whether the class is Diffable.  Diffable classes are handled specially.
            if (objectClass.isAnnotationPresent(Diffable.class)) {
                logger.log(Level.FINER, "{0} is Diffable", objectClass.getSimpleName());
                for (Field field : DiffUtils.getAllFields(objectClass)) {
                    // Only check fields annotated with DiffField.
                    if (field.isAnnotationPresent(DiffField.class)) {
                        Object fieldValue;
                        try {
                            fieldValue = DiffUtils.getValueForField(field, object);
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            logger.log(Level.SEVERE, "Error accessing field \"{0}\" in diff. Skipping.{1}", new Object[]{field.getName(), e});
                            continue;
                        }
                        // Resolve the data, in case some sort of lookup or any other processing is needed.
                        DiffField annotation = field.getAnnotation(DiffField.class);
                        String dataType = annotation.value();
                        DataResolver resolver;
                        synchronized (this) {
                            resolver = resolvers.get(dataType);
                        }
                        if (resolver != null) {
                            logger.finer("Resolving data...");
                            fieldValue = resolver.resolve(fieldValue);
                            logger.finer("Data resolved.");
                        }
                        // Recursively call resolveObject() on the two values, appending the field name to the tag.
                        returnValue.putAll(this.resolveObject(prefix + field.getName(), fieldValue));
                    }
                }
            } else {
                // For non-Diffable classes...

                logger.log(Level.FINER, "{0} is not Diffable.", objectClass.getSimpleName());
                // Iterate through iterable objects
                if (object instanceof Iterable) {
                    logger.log(Level.FINER, "{0} is Iterable.", objectClass.getSimpleName());
                    int i = 0;
                    for (Object o : ((Iterable<?>) object)) {
                        logger.log(Level.FINER, "Checking item with index: {0}", i);
                        // Recursively call resolveObject() on the corresponding values, appending the index.
                        returnValue.putAll(this.resolveObject(prefix + "idx" + ++i, o));
                    }
                    // Iterate through map keys
                } else if (object instanceof Map) {
                    Map<?, ?> oMap = (Map<?, ?>) object;
                    for (Object key : oMap.keySet()) {
                        Object obj = oMap.get(key);
                        // Recursively call resolveObject() on the corresponding vaues, appending the key.
                        returnValue.putAll(this.resolveObject(prefix + key.toString(), obj));
                    }
                    // If class isn't Diffable, not iterable, and not a map, simply add the object as a string
                } else {
                    returnValue.put(tag, object.toString());
                }
            }

        }

        return returnValue;
    }

    /**
     * Registers a {@link DataResolver DataResolver} to resolve data of type <code>forType</code>.
     * <p/>
     * The <code>calculate()</code> method can resolve data, using a <code>DataResolver</code>.  The field's
     * {@link DiffField DiffField} annotation can define a data type for the field, which the
     * <code>calculate()</code> method will then lookup in its registered resolvers, and pass the value
     * found in the actual field to this resolver, and use the result for the actual difference calculation.
     *
     * @param forType  the user-defined and application-specific field/data type to register a resolver for
     * @param resolver the resolver to register for the field/data type
     * @return the <code>DataResolver</code> previously registered for this data type, if any, or <code>null</code> otherwise
     * @see DataResolver
     * @see DiffGenerator#unregisterDataResolver(String)
     * @see DiffField
     */
    public synchronized DataResolver registerDataResolver(String forType, DataResolver resolver) {
        DataResolver old = resolvers.get(forType);
        resolvers.put(forType, resolver);
        return old;
    }

    /**
     * Unregisters a {@link DataResolver DataResolver}.
     *
     * @param forType the field/data type for which to unregister the resolver
     * @see DataResolver
     * @see DiffGenerator#registerDataResolver(String, DataResolver)
     * @see DiffField
     */
    public synchronized void unregisterDataResolver(String forType) {
        resolvers.remove(forType);
    }

    Map<String, String> calculate(Person a, Person b) {
        count++;
        return calculate("", a, b); 
    }

    Person apply(Person a, Diff diff) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
