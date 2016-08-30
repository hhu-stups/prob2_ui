package de.prob2.ui.groovy;

import java.io.IOException;
import java.lang.reflect.Field;
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
	private TableView<GroovyClassItem> tv_fields;
	
	@FXML
	private TableColumn<GroovyClassItem, String> fnames;
	
	@FXML
	private TableColumn<GroovyClassItem, String> fvalues;
	
	@FXML
	private TableColumn<GroovyClassItem, String> ftypes;
	
	@FXML
	private TableColumn<GroovyClassItem, String> fmodifiers;
	
	@FXML
	private TableColumn<GroovyClassItem, String> fdeclarers;
	
	@FXML
	private TableColumn<GroovyClassItem, String> mnames;
	
	@FXML
	private TableColumn<GroovyClassItem, String> mparams;
	
	@FXML
	private TableColumn<GroovyClassItem, String> mtypes;
		
	@FXML
	private TableColumn<GroovyClassItem, String> mmodifiers;
	
	@FXML
	private TableColumn<GroovyClassItem, String> mdeclarers;
	
	@FXML
	private TableColumn<GroovyClassItem, String> mexceptions;
	
	private ObservableList<GroovyClassItem> methods = FXCollections.observableArrayList();
	
	private ObservableList<GroovyClassItem> fields = FXCollections.observableArrayList();
	
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
		mnames.setCellValueFactory(new PropertyValueFactory<>("name"));
		mparams.setCellValueFactory(new PropertyValueFactory<>("params"));
		mtypes.setCellValueFactory(new PropertyValueFactory<>("type"));
		mmodifiers.setCellValueFactory(new PropertyValueFactory<>("modifier"));
		mdeclarers.setCellValueFactory(new PropertyValueFactory<>("declarer"));
		mexceptions.setCellValueFactory(new PropertyValueFactory<>("exception"));
		fnames.setCellValueFactory(new PropertyValueFactory<>("name"));
		fvalues.setCellValueFactory(new PropertyValueFactory<>("value"));
		ftypes.setCellValueFactory(new PropertyValueFactory<>("type"));
		fmodifiers.setCellValueFactory(new PropertyValueFactory<>("modifier"));
		fdeclarers.setCellValueFactory(new PropertyValueFactory<>("declarer"));
		
		tv_methods.setItems(methods);
		tv_fields.setItems(fields);
	}
	
	public void showMethodsAndFields() {
		methods.clear();
		fields.clear();
		for(Method m: clazz.getMethods()) {
			methods.add(new GroovyClassItem(m));
		}
		for(Field f : clazz.getFields()) {
			fields.add(new GroovyClassItem(f));
		}
		tv_methods.refresh();
		tv_fields.refresh();
	}

}
