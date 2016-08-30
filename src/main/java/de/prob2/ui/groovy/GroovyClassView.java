package de.prob2.ui.groovy;

import java.io.IOException;
import java.lang.reflect.Method;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;

public class GroovyClassView extends AnchorPane {
	
	private Class <? extends Object> clazz;
	
	@FXML
	private TableView<GroovyClassItem> tv_methods;
	
	@FXML
	private TableColumn<GroovyClassItem, String> names;
	
	@FXML
	private TableColumn<GroovyClassItem, String> params;
	
	@FXML
	private TableColumn<GroovyClassItem, String> types;
		
	@FXML
	private TableColumn<GroovyClassItem, String> modifiers;
	
	@FXML
	private TableColumn<GroovyClassItem, String> declarers;
	
	@FXML
	private TableColumn<GroovyClassItem, String> exceptions;
	
	private ObservableList<GroovyClassItem> methods = FXCollections.observableArrayList();
	
	public GroovyClassView(FXMLLoader loader) {
		loader.setLocation(getClass().getResource("groovy_class_view.fxml"));
		loader.setRoot(this);
		loader.setController(this);
		try {
			loader.load();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void setClass(Class <? extends Object> clazz) {
		this.clazz = clazz;
	}
	
	@FXML
	public void initialize() {
		names.setCellValueFactory(new PropertyValueFactory<>("name"));
		params.setCellValueFactory(new PropertyValueFactory<>("params"));
		types.setCellValueFactory(new PropertyValueFactory<>("type"));
		modifiers.setCellValueFactory(new PropertyValueFactory<>("modifier"));
		declarers.setCellValueFactory(new PropertyValueFactory<>("declarer"));
		exceptions.setCellValueFactory(new PropertyValueFactory<>("exception"));
		tv_methods.setItems(methods);

	}
	
	public void showMethods() {
		methods.clear();
		for(Method m: clazz.getMethods()) {
			methods.add(new GroovyClassItem(m));
		}
		tv_methods.refresh();
	}

}
