package de.prob2.ui.consoles.groovy.objects;

import de.prob2.ui.consoles.groovy.GroovyMethodOption;
import de.prob2.ui.internal.StageManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Tab;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.util.Collection;


public class GroovyClassStage extends Stage {
    private final ObservableList<GroovyClassPropertyItem> methods = FXCollections.observableArrayList();
    private final ObservableList<GroovyClassPropertyItem> fields = FXCollections.observableArrayList();
    private final ObservableList<GroovyClassItem> attributes = FXCollections.observableArrayList();
    private final ObservableList<CollectionDataItem> collectionData = FXCollections.observableArrayList();
    @FXML
    private TableView<GroovyClassPropertyItem> tvMethods;
    @FXML
    private TableView<GroovyClassPropertyItem> tvFields;
    @FXML
    private TableView<GroovyClassItem> tvClass;
    @FXML
    private TableView<CollectionDataItem> tvCollectionData;
    @FXML
    private Tab tabCollectionData;
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
    private Class<?> clazz;

    public GroovyClassStage(StageManager stageManager) {
        stageManager.loadFXML(this, "groovy_class_stage.fxml");
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
            GroovyClassHandler.handleCollections((Collection<?>) object, collectionData);
        } else {
            tabCollectionData.setDisable(true);
        }

        GroovyClassHandler.handleClassAttributes(clazz, attributes);
        tvMethods.refresh();
        tvFields.refresh();
        tvCollectionData.refresh();
        tvClass.refresh();
    }
}
