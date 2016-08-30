package de.prob2.ui.groovy;

import javafx.beans.property.SimpleStringProperty;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class GroovyObjectItem {
	
	private final SimpleStringProperty name;
	private final Class<? extends Object> clazz;
	private final SimpleStringProperty clazzname;
	private GroovyClassView classview;
	private Stage stage;
	
	public GroovyObjectItem(String name, Class<? extends Object> clazz, GroovyClassView classview) {
		this.name = new SimpleStringProperty(name);
		this.clazz = clazz;
		this.clazzname = new SimpleStringProperty(clazz.getSimpleName());
		this.classview = classview;
		classview.setClass(clazz);
		stage = new Stage();
		
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
		if(!classview.getStyleClass().isEmpty()) {
			stage.toFront();
			return;
		}
		stage.setTitle(clazz.getSimpleName());
		Scene scene = new Scene(classview, 800, 600);
		classview.showMethodsAndFields();
		stage.setScene(scene);
		stage.show();
	}
	
	public void close() {
		stage.close();
	}

}
