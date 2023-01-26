package de.prob2.ui.consoles.groovy.objects;


import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class GroovyObjectItem extends GroovyAbstractItem {
    private final Class<?> clazz;
    private final StringProperty clazzname;
    private final StringProperty value;
    private final Object object;
    private GroovyClassStage classstage;

    public GroovyObjectItem(String name, Object object, GroovyClassStage classstage) {
        super(name);
        this.object = object;
        this.clazz = object.getClass();
        this.clazzname = new SimpleStringProperty(this, "clazzname", clazz.getSimpleName());
        this.value = new SimpleStringProperty(this, "value", object.toString());
        if (classstage == null) {
            return;
        }
        this.classstage = classstage;
        classstage.setClass(clazz);
    }

    public String getClazzname() {
        return clazzname.get();
    }

    public void setClass(String clazzname) {
        this.clazzname.set(clazzname);
    }

    public String getValue() {
        return value.get();
    }

    public void setValue(String value) {
        this.value.set(value);
    }

    public void show() {
        classstage.setTitle(clazz.getSimpleName());
        classstage.showMethodsAndFields(object);
        classstage.show();
        classstage.toFront();
    }

    public void close() {
        classstage.close();
    }

    public GroovyClassStage getStage() {
        return classstage;
    }


}
