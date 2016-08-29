package de.prob2.ui.groovy;

import java.io.IOException;

import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

public class GroovyObjectItem {
	
	private final SimpleStringProperty name;
	private final Class<? extends Object> clazz;
	private final SimpleStringProperty clazzname;
	
	public GroovyObjectItem(String name, Class<? extends Object> clazz) {
		this.name = new SimpleStringProperty(name);
		this.clazz = clazz;
		this.clazzname = new SimpleStringProperty(clazz.getName());
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
		AnchorPane root = null;
		try {
			root = FXMLLoader.load(getClass().getResource("groovy_class_view.fxml"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Scene scene = new Scene(root, 800, 600);
		stage.setScene(scene);
		stage.show();
	}

}
