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
	private TableView<GroovyObjectItem> tv_objects;
	
	@FXML
	private TableColumn<GroovyObjectItem, String> objects;
	
	@FXML
	private TableColumn<GroovyObjectItem, String> classes;
	
	@FXML
	private TableColumn<GroovyObjectItem, String> values;
	
	private ObservableList<GroovyObjectItem> items = FXCollections.observableArrayList();
	
	private FXMLLoader loader;
	
	@Inject
	private GroovyObjectStage(FXMLLoader loader) {
		this.loader = loader;
		try {
			loader.setLocation(getClass().getResource("groovy_object_stage.fxml"));
			loader.setRoot(this);
			loader.setController(this);
			loader.load();
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.setOnCloseRequest(e-> {
			close();
		});
	}
	
	@Override
	public void close() {
		for(GroovyObjectItem item : items) {
			item.close();
		}
		super.close();
	}
	
	public void showObjects(ScriptEngine engine) {
		items.clear();
		fillList(engine.getBindings(ScriptContext.GLOBAL_SCOPE));
		fillList(engine.getBindings(ScriptContext.ENGINE_SCOPE));
		tv_objects.refresh();
		this.show();
	}
	
	private void fillList(Bindings binding) {
		int i = 0;
		for(String s : binding.keySet()) {
			Class <? extends Object> clazz = binding.values().toArray()[i].getClass();
			String value = binding.values().toArray()[i].toString();
			items.add(new GroovyObjectItem(s,clazz,value, new GroovyClassStage(loader)));
			i++;
		}	
	}
	
	@FXML
	public void initialize() {
		objects.setCellValueFactory(new PropertyValueFactory<>("name"));
		classes.setCellValueFactory(new PropertyValueFactory<>("clazzname"));
		values.setCellValueFactory(new PropertyValueFactory<>("value"));
		
		tv_objects.setItems(items);
		
		tv_objects.setOnMouseClicked(e-> {
			int currentPos = tv_objects.getSelectionModel().getSelectedIndex();
			if(currentPos >= 0) {
				items.get(currentPos).show();
			}
			tv_objects.getSelectionModel().clearSelection();
		});
	}
	
}
