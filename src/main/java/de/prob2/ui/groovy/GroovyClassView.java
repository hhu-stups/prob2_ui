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
	private TableView<GroovyClassPropertyItem> tv_methods;
	
	@FXML
	private TableView<GroovyClassPropertyItem> tv_fields;
	
	@FXML
	private TableView<GroovyClassItem> tv_class;
	
	@FXML
	private TableColumn<GroovyClassItem, String> cattributes;
	
	@FXML
	private TableColumn<GroovyClassItem, String> cvalues;
	
	@FXML
	private TableColumn<GroovyClassPropertyItem, String> fnames;
	
	@FXML
	private TableColumn<GroovyClassPropertyItem, String> fvalues;
	
	@FXML
	private TableColumn<GroovyClassPropertyItem, String> ftypes;
	
	@FXML
	private TableColumn<GroovyClassPropertyItem, String> fmodifiers;
	
	@FXML
	private TableColumn<GroovyClassPropertyItem, String> fdeclarers;
	
	@FXML
	private TableColumn<GroovyClassPropertyItem, String> mnames;
	
	@FXML
	private TableColumn<GroovyClassPropertyItem, String> mparams;
	
	@FXML
	private TableColumn<GroovyClassPropertyItem, String> mtypes;
		
	@FXML
	private TableColumn<GroovyClassPropertyItem, String> mmodifiers;
	
	@FXML
	private TableColumn<GroovyClassPropertyItem, String> mdeclarers;
	
	@FXML
	private TableColumn<GroovyClassPropertyItem, String> mexceptions;
	
	private ObservableList<GroovyClassPropertyItem> methods = FXCollections.observableArrayList();
	
	private ObservableList<GroovyClassPropertyItem> fields = FXCollections.observableArrayList();
	
	private ObservableList<GroovyClassItem> attributes = FXCollections.observableArrayList();
	
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
		cattributes.setCellValueFactory(new PropertyValueFactory<>("attribute"));
		cvalues.setCellValueFactory(new PropertyValueFactory<>("value"));
		
		tv_methods.setItems(methods);
		tv_fields.setItems(fields);
		tv_class.setItems(attributes);
	}
	
	public void showMethodsAndFields() {
		methods.clear();
		fields.clear();
		for(Method m: clazz.getMethods()) {
			methods.add(new GroovyClassPropertyItem(m));
		}
		for(Field f : clazz.getFields()) {
			fields.add(new GroovyClassPropertyItem(f));
		}
		showClassAttributes();
		tv_methods.refresh();
		tv_fields.refresh();
	}
	
	private void showClassAttributes() {
		attributes.clear();
		attributes.add(new GroovyClassItem("Package", clazz.getPackage().getName()));
		attributes.add(new GroovyClassItem("Class Name", clazz.getName()));
		String interfaces = "";
		String superclasses ="";
		for(Class<? extends Object> c : clazz.getInterfaces()) {
			interfaces += c.getSimpleName() + ", ";
		}
		interfaces = interfaces.substring(0, Math.max(0,interfaces.length() - 2));
		attributes.add(new GroovyClassItem("Interfaces", interfaces));
		do {
			superclasses += clazz.getSuperclass().getSimpleName() + ", ";
		} while(!(clazz.getSuperclass() instanceof Object));
		superclasses = superclasses.substring(0, Math.max(0,superclasses.length() - 2));
		attributes.add(new GroovyClassItem("Superclasses", superclasses));
		attributes.add(new GroovyClassItem("isPrimitive", new Boolean(clazz.isPrimitive()).toString()));
		attributes.add(new GroovyClassItem("isArray", new Boolean(clazz.isArray()).toString()));
		tv_class.refresh();
	}

}
