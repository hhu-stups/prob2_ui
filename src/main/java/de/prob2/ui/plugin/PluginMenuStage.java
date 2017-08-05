package de.prob2.ui.plugin;

import com.google.inject.Inject;
import de.prob2.ui.internal.StageManager;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ResourceBundle;

/**
 * Created by Christoph Heinzen on 03.08.17.
 */
public class PluginMenuStage extends Stage {

    private static final Logger LOGGER = LoggerFactory.getLogger(PluginMenuStage.class);

    private final PluginManager pluginManager;
    private final ResourceBundle bundle;

    @FXML
    private TableView pluginTableView;
    @FXML
    private TableColumn<Plugin, String> nameCol;
    @FXML
    private TableColumn<Plugin, String> versionCol;
    @FXML
    private TableColumn<Plugin, Boolean> activeCol;

    @Inject
    public PluginMenuStage(final StageManager stageManager, final PluginManager pluginManager, final ResourceBundle bundle) {
        this.pluginManager = pluginManager;
        this.bundle = bundle;
        stageManager.loadFXML(this, "plugin_menu_stage.fxml");
    }

    @FXML
    private void initialize() {
        ObservableMap<Plugin, Boolean> activePlugins = pluginManager.getActivePlugins();
        final ObservableList<Plugin> pluginList = FXCollections.observableArrayList(activePlugins.keySet());

        MapChangeListener<Plugin, Boolean> activePluginsListener = change -> {
                    boolean removed = change.wasRemoved();
                    if (removed != change.wasAdded()) {
                        // if both are false nothing happened and if both are true a value was overwritten
                        // in both cases we don't need to react
                        if (removed) {
                            // a key-value pair was removed
                            pluginList.remove(change.getKey());
                        } else {
                            // a key-value pair was added
                            pluginList.add(change.getKey());
                        }
                    }
                };
        activePlugins.addListener(activePluginsListener);
        this.setOnCloseRequest(closeEvent -> activePlugins.removeListener(activePluginsListener));

        nameCol.setCellFactory(TextFieldTableCell.forTableColumn());
        nameCol.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getName()));

        versionCol.setCellFactory(param -> {
            TextFieldTableCell<Plugin, String> cell = new TextFieldTableCell<>();
            cell.setAlignment(Pos.CENTER);
            return cell;
        });
        versionCol.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getVersion()));

        activeCol.setCellFactory(param -> {
            CheckBoxTableCell<Plugin, Boolean> cell = new CheckBoxTableCell<>();
            cell.setAlignment(Pos.CENTER);
            return cell;
        });
        activeCol.setCellValueFactory(param -> {
            final Plugin plugin = param.getValue();
            SimpleBooleanProperty booleanProp = new SimpleBooleanProperty(activePlugins.get(plugin));
            booleanProp.addListener((observable, oldValue, newValue) -> activePlugins.put(plugin, newValue));
            return booleanProp;
        });

        pluginTableView.setItems(pluginList);

    }

    @FXML
    private void handlePluginSearch() {

    }

    @FXML
    private void addPlugin() {
       pluginManager.addPlugin();
    }

}
