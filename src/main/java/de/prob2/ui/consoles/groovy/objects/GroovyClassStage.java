package de.prob2.ui.consoles.groovy.objects;

import java.util.Collection;

import de.prob2.ui.consoles.groovy.GroovyMethodOption;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.persistence.UIState;


import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;


public class GroovyClassStage extends Stage {
	@FXML private TableView<GroovyClassPropertyItem> tvMethods;
	@FXML private TableView<GroovyClassPropertyItem> tvFields;
	@FXML private TableView<GroovyClassItem> tvClass;
	@FXML private TableView<CollectionDataItem> tvCollectionData;
	
	@FXML private TabPane tabPane;
	@FXML private Tab tabCollectionData;
	@FXML private Tab tabClass;
	@FXML private Tab tabFields;
	@FXML private Tab tabMethods;
	
	@FXML private TableColumn<GroovyClassItem, String> cattributes;
	@FXML private TableColumn<GroovyClassItem, String> cvalues;
	
	@FXML private TableColumn<GroovyClassPropertyItem, String> fnames;
	@FXML private TableColumn<GroovyClassPropertyItem, String> fvalues;
	@FXML private TableColumn<GroovyClassPropertyItem, String> ftypes;
	@FXML private TableColumn<GroovyClassPropertyItem, String> forigins;
	@FXML private TableColumn<GroovyClassPropertyItem, String> fmodifiers;
	@FXML private TableColumn<GroovyClassPropertyItem, String> fdeclarers;
	
	@FXML private TableColumn<GroovyClassPropertyItem, String> mnames;
	@FXML private TableColumn<GroovyClassPropertyItem, String> mparams;
	@FXML private TableColumn<GroovyClassPropertyItem, String> mtypes;
	@FXML private TableColumn<GroovyClassPropertyItem, String> morigins;
	@FXML private TableColumn<GroovyClassPropertyItem, String> mmodifiers;
	@FXML private TableColumn<GroovyClassPropertyItem, String> mdeclarers;
	@FXML private TableColumn<GroovyClassPropertyItem, String> mexceptions;
	
	@FXML private TableColumn<CollectionDataItem, String> cdindices;
	@FXML private TableColumn<CollectionDataItem, String> cdvalues;
	
	private Class<?> clazz;
	
	private ObservableList<GroovyClassPropertyItem> methods = FXCollections.observableArrayList();
	private ObservableList<GroovyClassPropertyItem> fields = FXCollections.observableArrayList();
	private ObservableList<GroovyClassItem> attributes = FXCollections.observableArrayList();
	private ObservableList<CollectionDataItem> collectionData = FXCollections.observableArrayList();
	
	private UIState uiState;
	private int index;

	
	public GroovyClassStage(StageManager stageManager, UIState uiState, String name) {
		this.index = 0;
		this.uiState = uiState;
		stageManager.loadFXML(this, "groovy_class_stage.fxml", "#GroovyObjectId:" + name);
	}
	
	public void setClass(Class<?> clazz) {
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
		
		tvMethods.setItems(methods);
		tvFields.setItems(fields);
		tvClass.setItems(attributes);
		tvCollectionData.setItems(collectionData);

		tabPane.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> uiState.getGroovyObjectTabs().set(index, newValue.getText()));
		
		this.setOnCloseRequest(e -> uiState.removeGroovyObjectTab(index));
	}
	
	public void showMethodsAndFields(Object object) {
		methods.clear();
		fields.clear();
		collectionData.clear();
		
		GroovyClassHandler.handleProperties(clazz, fields, object);
		GroovyClassHandler.handleMethods(clazz, methods, GroovyMethodOption.ALL);
		
		if (clazz.isArray()) {
			GroovyClassHandler.handleArrays(object, collectionData);
		} else if (object instanceof Collection<?>) {
			GroovyClassHandler.handleCollections((Collection<?>)object, collectionData);
		} else {
			tabCollectionData.setDisable(true);
		}
		
		GroovyClassHandler.handleClassAttributes(clazz, attributes);
		tvMethods.refresh();
		tvFields.refresh();
		tvCollectionData.refresh();
		tvClass.refresh();
	}
	
	public void openTab(String tab) {
		switch(tab) {
			case "Public Fields and Properties":
				tabPane.getSelectionModel().select(tabFields);
				break;
			case "Methods":
				tabPane.getSelectionModel().select(tabMethods);
				break;
			default:
				tabPane.getSelectionModel().select(tabClass);
		}
	}
	
	public void setIndex(int index) {
		this.index = index;
	}
}
