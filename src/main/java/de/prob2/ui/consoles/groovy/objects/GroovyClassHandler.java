package de.prob2.ui.consoles.groovy.objects;

import de.prob2.ui.consoles.groovy.GroovyMethodOption;
import groovy.lang.MetaMethod;
import groovy.lang.MetaProperty;
import groovy.lang.PropertyValue;
import javafx.collections.ObservableList;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class GroovyClassHandler {
    private GroovyClassHandler() {
        throw new IllegalStateException("Utility class");
    }

    public static void handleProperties(Class<?> clazz, ObservableList<GroovyClassPropertyItem> fields, Object object) {
        for (Field f : clazz.getFields()) {
            fields.add(new GroovyClassPropertyItem(f));
        }
        for (PropertyValue p : DefaultGroovyMethods.getMetaPropertyValues(object)) {
            fields.add(new GroovyClassPropertyItem(p));
        }
    }

    public static void handleProperties(Class<?> clazz, Collection<? super GroovyClassPropertyItem> properties) {
        for (MetaProperty m : DefaultGroovyMethods.getMetaClass(clazz).getProperties()) {
            properties.add(new GroovyClassPropertyItem(m));
        }
    }


    public static void handleArrays(Object object, ObservableList<CollectionDataItem> collectionData) {
        if (object instanceof Object[]) {
            // Check Array of Objects
            Object[] objects = (Object[]) object;
            for (int i = 0; i < objects.length; i++) {
                String value = "";
                if (objects[i] != null) {
                    value = objects[i].toString();
                }
                collectionData.add(new CollectionDataItem(i, value));
            }
        } else {
            // Check Array of Primitives
            int length = Array.getLength(object);
            for (int i = 0; i < length; i++) {
                collectionData.add(new CollectionDataItem(i, Array.get(object, i)));
            }
        }
    }

    public static void handleCollections(Collection<?> object, ObservableList<CollectionDataItem> collectionData) {
        int i = 0;
        for (Object o : object) {
            collectionData.add(new CollectionDataItem(i, o));
            i++;
        }
    }

    public static void handleMethods(Class<?> clazz, Collection<? super GroovyClassPropertyItem> methods, GroovyMethodOption option) {
        for (Method m : clazz.getMethods()) {
            GroovyClassPropertyItem newItem = new GroovyClassPropertyItem(m);
            if (!methods.contains(newItem)) {
                methods.add(new GroovyClassPropertyItem(m));
            }
        }

        for (MetaMethod m : DefaultGroovyMethods.getMetaClass(clazz).getMetaMethods()) {
            if ((option == GroovyMethodOption.ALL) || (option == GroovyMethodOption.NONSTATIC && !m.isStatic()) || (option == GroovyMethodOption.STATIC && !m.isStatic())) {
                GroovyClassPropertyItem newItem = new GroovyClassPropertyItem(m);
                if (!methods.contains(newItem)) {
                    methods.add(new GroovyClassPropertyItem(m));
                }
            }
        }

    }

    public static void handleClassAttributes(Class<?> clazz, ObservableList<GroovyClassItem> attributes) {
        attributes.clear();
        String packagename = "default";
        if (clazz.getPackage() != null) {
            packagename = clazz.getPackage().getName();
        }
        attributes.add(new GroovyClassItem("Package", packagename));
        attributes.add(new GroovyClassItem("Class Name", clazz.getName()));

        final List<String> interfaceNames = new ArrayList<>();
        for (Class<?> c : clazz.getInterfaces()) {
            interfaceNames.add(c.getSimpleName());
        }
        attributes.add(new GroovyClassItem("Interfaces", String.join(", ", interfaceNames)));

        final List<String> superclassNames = new ArrayList<>();
        Class<?> tmp = clazz;
        while (!Object.class.equals(tmp)) {
            superclassNames.add(tmp.getSuperclass().getSimpleName());
            tmp = tmp.getSuperclass();
        }
        attributes.add(new GroovyClassItem("Superclasses", String.join(", ", superclassNames)));
        attributes.add(new GroovyClassItem("isPrimitive", Boolean.toString(clazz.isPrimitive())));
        attributes.add(new GroovyClassItem("isArray", Boolean.toString(clazz.isArray())));

    }


}
