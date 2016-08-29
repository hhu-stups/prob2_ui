package de.prob2.ui.groovy;

import java.io.IOException;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import com.google.inject.Inject;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

public class GroovyObjectStage extends Stage {
	
	@FXML
	TableView<GroovyObjectItem> tv_objects;
	
	@FXML
	TableColumn<GroovyObjectItem, String> objects;
	
	ObservableList<GroovyObjectItem> values = FXCollections.observableArrayList();
	
	@Inject
	private GroovyObjectStage(FXMLLoader loader) {
		try {
			loader.setLocation(getClass().getResource("groovy_object_stage.fxml"));
			loader.setRoot(this);
			loader.setController(this);
			loader.load();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void showObjects(ScriptEngine engine) {
		Bindings binding = engine.getBindings(ScriptContext.ENGINE_SCOPE);
		values.clear();
		for(String s : binding.keySet()) {
			values.add(new GroovyObjectItem(s));
		}
		tv_objects.refresh();
		this.show();
	}
	
	@FXML
	public void initialize() {
		objects.setCellValueFactory(new PropertyValueFactory<>("name"));
		tv_objects.setItems(values);
	}
	
}
