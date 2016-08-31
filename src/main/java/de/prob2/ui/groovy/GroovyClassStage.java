package de.prob2.ui.groovy;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Tab;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

public class GroovyClassStage extends Stage {
	
	private Class <? extends Object> clazz;
	
	@FXML
	private TableView<GroovyClassPropertyItem> tv_methods;
	
	@FXML
	private TableView<GroovyClassPropertyItem> tv_fields;
	
	@FXML
	private TableView<GroovyClassItem> tv_class;
	
	@FXML
	private TableView<CollectionDataItem> tv_collection_data;
	
	@FXML
	private Tab tab_collection_data;
	
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
	private TableColumn<GroovyClassPropertyItem, String> forigins;
	
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
	private TableColumn<GroovyClassPropertyItem, String> morigins;
		
	@FXML
	private TableColumn<GroovyClassPropertyItem, String> mmodifiers;
	
	@FXML
	private TableColumn<GroovyClassPropertyItem, String> mdeclarers;
	
	@FXML
	private TableColumn<GroovyClassPropertyItem, String> mexceptions;
	
	@FXML
	private TableColumn<CollectionDataItem, String> cdindices;
	
	@FXML
	private TableColumn<CollectionDataItem, String> cdvalues;
	
	private ObservableList<GroovyClassPropertyItem> methods = FXCollections.observableArrayList();
	
	private ObservableList<GroovyClassPropertyItem> fields = FXCollections.observableArrayList();
	
	private ObservableList<GroovyClassItem> attributes = FXCollections.observableArrayList();
	
	private ObservableList<CollectionDataItem> collection_data = FXCollections.observableArrayList();
	
	private MetaPropertiesHandler groovyHandler;
	
	public GroovyClassStage(FXMLLoader loader, MetaPropertiesHandler groovyHandler) {
		loader.setLocation(getClass().getResource("groovy_class_stage.fxml"));
		loader.setRoot(this);
		loader.setController(this);
		try {
			loader.load();
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.groovyHandler = groovyHandler;
	}
	
	public void setClass(Class <? extends Object> clazz) {
		this.clazz = clazz;
	}
	
	
	
	@FXML
	public void initialize() {
		mnames.setCellValueFactory(new PropertyValueFactory<>("name"));
		mparams.setCellValueFactory(new PropertyValueFactory<>("params"));
		mtypes.setCellValueFactory(new PropertyValueFactory<>("type"));
		morigins.setCellValueFactory(new PropertyValueFactory<>("origin"));
		mmodifiers.setCellValueFactory(new PropertyValueFactory<>("modifier"));
		mdeclarers.setCellValueFactory(new PropertyValueFactory<>("declarer"));
		mexceptions.setCellValueFactory(new PropertyValueFactory<>("exception"));
		fnames.setCellValueFactory(new PropertyValueFactory<>("name"));
		fvalues.setCellValueFactory(new PropertyValueFactory<>("value"));
		ftypes.setCellValueFactory(new PropertyValueFactory<>("type"));
		forigins.setCellValueFactory(new PropertyValueFactory<>("origin"));
		fmodifiers.setCellValueFactory(new PropertyValueFactory<>("modifier"));
		fdeclarers.setCellValueFactory(new PropertyValueFactory<>("declarer"));
		cattributes.setCellValueFactory(new PropertyValueFactory<>("attribute"));
		cvalues.setCellValueFactory(new PropertyValueFactory<>("value"));
		cdindices.setCellValueFactory(new PropertyValueFactory<>("index"));
		cdvalues.setCellValueFactory(new PropertyValueFactory<>("value"));
		
		tv_methods.setItems(methods);
		tv_fields.setItems(fields);
		tv_class.setItems(attributes);
		tv_collection_data.setItems(collection_data);
	}
	
	public void showMethodsAndFields(Object object) {
		methods.clear();
		fields.clear();
		collection_data.clear();
		
		for(Method m: clazz.getMethods()) {
			methods.add(new GroovyClassPropertyItem(m));
		}
		for(Field f : clazz.getFields()) {
			fields.add(new GroovyClassPropertyItem(f));
		}
		groovyHandler.handleProperties(object, fields);
		groovyHandler.handleMethods(object, methods);
		handleCollections(object);
		showClassAttributes();
		tv_methods.refresh();
		tv_fields.refresh();
		tv_collection_data.refresh();
	}
	
	private void handleCollections(Object object) {
		int i = 0;
		if(clazz.isArray()) {
			Object[] objects = (Object[]) object;
			for(i = 0; i < objects.length; i++) {
				String value ="";
				if(objects[i] != null) {
					value = objects[i].toString();
				}
				collection_data.add(new CollectionDataItem(i,value));
			}
		} else if(Collection.class.isAssignableFrom(object.getClass())) {
			for(Object o: (Iterable<? extends Object>) object) {
				collection_data.add(new CollectionDataItem(i,o));
				i++;
			}
		} else {
			tab_collection_data.setDisable(true);
		}
	}
	
	private void showClassAttributes() {
		attributes.clear();
		String packagename = "default";
		if(clazz.getPackage() != null) {
			packagename = clazz.getPackage().getName();
		}
		attributes.add(new GroovyClassItem("Package", packagename));
		attributes.add(new GroovyClassItem("Class Name", clazz.getName()));
		String interfaces = "";
		String superclasses ="";
		for(Class<? extends Object> c : clazz.getInterfaces()) {
			interfaces += c.getSimpleName() + ", ";
		}
		interfaces = interfaces.substring(0, Math.max(0,interfaces.length() - 2));
		attributes.add(new GroovyClassItem("Interfaces", interfaces));
		Class <? extends Object> tmp = clazz;
		do{
			superclasses += tmp.getSuperclass().getSimpleName() + ", ";
			tmp = tmp.getSuperclass();
		} while(!tmp.getName().equals("java.lang.Object"));
		superclasses = superclasses.substring(0, Math.max(0,superclasses.length() - 2));
		attributes.add(new GroovyClassItem("Superclasses", superclasses));
		attributes.add(new GroovyClassItem("isPrimitive", new Boolean(clazz.isPrimitive()).toString()));
		attributes.add(new GroovyClassItem("isArray", new Boolean(clazz.isArray()).toString()));
		tv_class.refresh();
	}

}
