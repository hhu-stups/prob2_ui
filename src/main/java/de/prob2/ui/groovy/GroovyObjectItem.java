package de.prob2.ui.groovy;

import javafx.beans.property.SimpleStringProperty;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class GroovyObjectItem {
	
	private final SimpleStringProperty name;
	private final Class<? extends Object> clazz;
	private final SimpleStringProperty clazzname;
	private GroovyClassView classview;
	
	public GroovyObjectItem(String name, Class<? extends Object> clazz, GroovyClassView classview) {
		this.name = new SimpleStringProperty(name);
		this.clazz = clazz;
		this.clazzname = new SimpleStringProperty(clazz.getName());
		this.classview = classview;
	}
	
	public String getName() {
		return name.get();
	}
	
	public void setName(String name) {
		this.name.set(name);
	}
	
	public String getClazzname() {
		return clazzname.get();
	}
	
	public void setClass(String clazzname) {
		this.clazzname.set(clazzname);
	}
	
	public void show() {
		Stage stage = new Stage();
		stage.setTitle(clazz.toString());
		Scene scene = new Scene(classview, 800, 600);
		stage.setScene(scene);
		stage.show();
	}

}
