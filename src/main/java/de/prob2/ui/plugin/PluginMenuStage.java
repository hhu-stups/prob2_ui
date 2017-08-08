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
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.ResourceBundle;
import java.util.function.Predicate;

/**
 * Created by Christoph Heinzen on 03.08.17.
 */
public class PluginMenuStage extends Stage {

    private static final Logger LOGGER = LoggerFactory.getLogger(PluginMenuStage.class);

    private final PluginManager pluginManager;
    private final ResourceBundle bundle;
    private final StageManager stageManager;

    @FXML
    private TableView<Plugin> pluginTableView;
    @FXML
    private TextField pluginSearchTextField;
    @FXML
    private TableColumn<Plugin, String> nameCol;
    @FXML
    private TableColumn<Plugin, String> versionCol;
    @FXML
    private TableColumn<Plugin, Boolean> activeCol;

    @Inject
    public PluginMenuStage(final StageManager stageManager,
                           final PluginManager pluginManager,
                           final ResourceBundle bundle) {
        this.pluginManager = pluginManager;
        this.bundle = bundle;
        this.stageManager = stageManager;
        stageManager.loadFXML(this, "plugin_menu_stage.fxml");
    }

    @FXML
    private void initialize() {
        ObservableMap<Plugin, Boolean> activePlugins = pluginManager.getActivePlugins();
        final ObservableList<Plugin> pluginList = FXCollections.observableArrayList(activePlugins.keySet());
        pluginList.sort(Comparator.comparing(Plugin::getName));

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

        configureColumns(activePlugins);
        configureContextMenu();

        FilteredList<Plugin> pluginFilteredList = new FilteredList<>(pluginList,p -> true);
        pluginSearchTextField.textProperty().addListener((observable, oldValue, newValue) ->
                pluginFilteredList.setPredicate(plugin -> {
                    if (newValue == null || newValue.isEmpty()) {
                        return true;
                    }
                    return plugin.getName().toLowerCase().contains(newValue.toLowerCase());
        }));

        SortedList<Plugin> pluginSortedFilteredList = new SortedList<>(pluginFilteredList, Comparator.comparing(Plugin::getName));
        pluginSortedFilteredList.comparatorProperty().bind(pluginTableView.comparatorProperty());
        pluginTableView.setItems(pluginSortedFilteredList);
    }

    @FXML
    private void addPlugin() {
       pluginManager.addPlugin(this);
    }

    @FXML
    private void reloadPlugins() {
        pluginManager.loadPlugins();
    }

    private void configureColumns(ObservableMap<Plugin, Boolean> activePlugins) {
        nameCol.setCellFactory(TextFieldTableCell.forTableColumn());
        nameCol.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getName()));
        nameCol.setSortType(TableColumn.SortType.ASCENDING);

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
            booleanProp.addListener((observable, oldValue, newValue) -> {
                if (newValue) {
                    try {
                        plugin.start(pluginManager);
                        activePlugins.put(plugin, true);
                    } catch (Exception e) {
                        LOGGER.warn("Could not start the plugin {}. The following exception was thrown:", plugin.getName(), e);
                        activePlugins.put(plugin, false);
                    }
                } else {
                    try {
                        plugin.stop();
                        activePlugins.put(plugin, false);
                    } catch (Exception e) {
                        LOGGER.warn("Could not stop the plugin {}. The following exception was thrown:", plugin.getName(), e);
                        activePlugins.put(plugin, true);
                    }
                }});
            return booleanProp;
        });
    }

    private void configureContextMenu() {
        pluginTableView.addEventHandler(MouseEvent.MOUSE_CLICKED, mouseEvent -> {
            if (mouseEvent.getButton() == MouseButton.SECONDARY) {
                Plugin plugin = pluginTableView.getSelectionModel().getSelectedItem();
                if (plugin != null) {
                    MenuItem restartItem = new MenuItem(
                            String.format(bundle.getString("pluginsmenu.table.contextmenu.restart"), plugin.getName()));
                    restartItem.setOnAction(event -> {
                        plugin.stop();
                        pluginManager.getActivePlugins().put(plugin, false);
                        plugin.start(pluginManager);
                        pluginManager.getActivePlugins().put(plugin, true);
                    });
                    MenuItem removeMenuItem = new MenuItem(
                            String.format(bundle.getString("pluginsmenu.table.contextmenu.remove"), plugin.getName()));
                    removeMenuItem.setOnAction(event -> {
                        Alert dialog = stageManager.makeAlert(Alert.AlertType.CONFIRMATION,
                                String.format(bundle.getString("pluginsmenu.table.dialog.remove.question"), plugin.getName()),
                                ButtonType.YES, ButtonType.NO);
                        dialog.setTitle(bundle.getString("pluginsmenu.table.dialog.title"));
                        dialog.showAndWait().ifPresent(response -> {
                            if (response == ButtonType.YES) {
                                pluginManager.removePlugin(plugin);
                            }
                        });
                    });
                    new ContextMenu(restartItem, removeMenuItem)
                            .show(pluginTableView, mouseEvent.getScreenX(), mouseEvent.getScreenY());
                }
            }
        });
    }
}
