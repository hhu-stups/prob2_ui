package de.prob2.ui.consoles.groovy.objects;


import de.prob2.ui.persistence.UIState;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class GroovyObjectItem extends GroovyAbstractItem {
	
	public enum ShowEnum {
		DEFAULT,PERSISTENCE
	}
	
	private final Class<?> clazz;
	private final StringProperty clazzname;
	private final StringProperty value;
	private final Object object;
	private GroovyClassStage classstage;
	private final UIState uiState;
	
	public GroovyObjectItem(String name, Object object, GroovyClassStage classstage, UIState uiState) {
		super(name);
		this.uiState = uiState;
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
	
	public void show(ShowEnum openBy, int index) {
		classstage.setTitle(clazz.getSimpleName());
		classstage.showMethodsAndFields(object);
		classstage.show();
		if (openBy == ShowEnum.DEFAULT) {
			uiState.addGroovyObjectTab("Class");
		} else {
			classstage.openTab(uiState.getGroovyObjectTabs().get(index));
			classstage.setIndex(index);
			return;
		}
		classstage.setIndex(uiState.getGroovyObjectTabs().size() - 1);
		classstage.toFront();
	}
	
	public void close() {
		classstage.close();
	}
	
	public GroovyClassStage getStage() {
		return classstage;
	}
	

}
