package de.prob2.ui.plugin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.MouseButton;
import javafx.stage.Stage;

import org.pf4j.PluginState;
import org.pf4j.PluginStateListener;
import org.pf4j.PluginWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public final class PluginMenuStage extends Stage {
	private static final Logger LOGGER = LoggerFactory.getLogger(PluginMenuStage.class);

	private final ProBPluginManager proBPluginManager;
	private final I18n i18n;
	private final StageManager stageManager;

	private ObservableList<PluginWrapper> pluginList;

	@FXML
	private TableView<PluginWrapper> pluginTableView;
	@FXML
	private TextField pluginSearchTextField;
	@FXML
	private TableColumn<PluginWrapper, String> nameCol;
	@FXML
	private TableColumn<PluginWrapper, String> versionCol;
	@FXML
	private TableColumn<PluginWrapper, Boolean> activeCol;
	@FXML
	private TextField pathTextField;

	private PluginStateListener stateListener;

	@Inject
	public PluginMenuStage(final StageManager stageManager,
						   final ProBPluginManager proBPluginManager,
						   final I18n i18n) {
		this.proBPluginManager = proBPluginManager;
		this.i18n = i18n;
		this.stageManager = stageManager;
		stageManager.loadFXML(this, "plugin_menu_stage.fxml", this.getClass().getName());
	}

	@FXML
	private void initialize() {
		pathTextField.setText(proBPluginManager.getPluginDirectory().toString());

		pluginList = FXCollections.observableArrayList();

		stateListener = event -> {
			final PluginWrapper plugin = event.getPlugin();
			if (plugin.getPlugin() != null) {
				if (pluginList.contains(plugin) && !getProBJarPluginManager().getPlugins().contains(plugin)) {
					// a plugin was removed
					pluginList.remove(plugin);
				} else if (!pluginList.contains(plugin) && getProBJarPluginManager().getPlugins().contains(plugin)) {
					// a new plugin was added
					pluginList.add(plugin);
					FXCollections.sort(pluginList, Comparator.comparing(o -> ((ProBPlugin) o.getPlugin()).getName()));
				}
			}
		};

		getProBJarPluginManager().addPluginStateListener(stateListener);

		configureColumns();
		configureContextMenu();

		final FilteredList<PluginWrapper> pluginFilteredList = new FilteredList<>(pluginList, p -> true);
		pluginSearchTextField.textProperty().addListener((observable, oldValue, newValue) ->
			pluginFilteredList.setPredicate(
				plugin -> newValue == null ||
						newValue.isEmpty() ||
						((ProBPlugin) plugin.getPlugin()).getName().toLowerCase().contains(newValue.toLowerCase())
			)
		);

		pluginTableView.setItems(pluginFilteredList);
		pluginTableView.setSelectionModel(null);

		pluginList.addAll(getProBJarPluginManager().getPlugins());
	}

	@FXML
	private void addPlugin() {
		proBPluginManager.addPlugin();
	}

	@FXML
	private void reloadPlugins() {
		proBPluginManager.reloadPlugins();
		pluginList.clear();
		pluginList.addAll(getProBJarPluginManager().getPlugins());
		//TODO: give the user the information what happened
	}

	@FXML
	private void changePath() {
		final List<PluginWrapper> plugins = proBPluginManager.changePluginDirectory();
		if (plugins != null) {
			pluginList.clear();
			pluginList.addAll(plugins);
			getProBJarPluginManager().addPluginStateListener(stateListener);
		}
		pathTextField.setText(proBPluginManager.getPluginDirectory().toString());
	}

	private void configureColumns() {
		nameCol.setCellFactory(TextFieldTableCell.forTableColumn());
		nameCol.setCellValueFactory(param -> new SimpleStringProperty(((ProBPlugin)param.getValue().getPlugin()).getName()));
		nameCol.setSortable(false);

		versionCol.setCellFactory(param -> {
			final TextFieldTableCell<PluginWrapper, String> cell = new TextFieldTableCell<>();
			cell.setAlignment(Pos.CENTER);
			return cell;
		});
		versionCol.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getDescriptor().getVersion()));
		versionCol.setSortable(false);

		activeCol.setCellFactory(param -> {
			final CheckBoxTableCell<PluginWrapper, Boolean> cell = new CheckBoxTableCell<>();
			cell.setAlignment(Pos.CENTER);
			return cell;
		});
		activeCol.setCellValueFactory(param -> {
			final PluginWrapper plugin = param.getValue();
			final SimpleBooleanProperty booleanProp = new SimpleBooleanProperty(plugin.getPluginState() == PluginState.STARTED);
			booleanProp.addListener((observable, oldValue, newValue) -> {
				if (newValue) {
					getProBJarPluginManager().startPlugin(plugin.getPluginId());
				} else {
					getProBJarPluginManager().stopPlugin(plugin.getPluginId());
				}
				try {
					proBPluginManager.writeInactivePlugins();
				} catch (final IOException e) {
					LOGGER.error("Failed to write list of inactive plugins", e);
					final Alert alert = stageManager.makeExceptionAlert(e, "plugin.alerts.couldNotWriteInactive.content");
					alert.initOwner(this);
					alert.show();
				}
			});
			return booleanProp;
		});
		activeCol.setSortable(false);
	}

	private void configureContextMenu() {
		pluginTableView.setRowFactory(tableView -> {
			final TableRow<PluginWrapper> row = new TableRow<>();
			row.setOnMouseClicked(clickEvent -> {
				if(clickEvent.getButton() == MouseButton.SECONDARY && row.getItem() != null) {
					final ContextMenu ctMenu = createContextMenu(row.getItem());
					ctMenu.show(row, clickEvent.getScreenX(), clickEvent.getScreenY());
				}
			});
			return row;
		});
	}

	private ContextMenu createContextMenu(final PluginWrapper pluginWrapper) {
		final ProBPlugin plugin = (ProBPlugin) pluginWrapper.getPlugin();
		final String pluginId = pluginWrapper.getPluginId();
		final String pluginName = plugin.getName();

		final MenuItem restartItem = new MenuItem(i18n.translate("plugin.pluginMenu.table.contextmenu.restart", pluginName));
		restartItem.setOnAction(event -> {
			if (PluginState.STOPPED == getProBJarPluginManager().stopPlugin(pluginId)) {
				getProBJarPluginManager().startPlugin(pluginId);
			}
		});

		final MenuItem removeMenuItem = new MenuItem(i18n.translate("plugin.pluginMenu.table.contextmenu.remove", pluginName));
		removeMenuItem.setOnAction(event -> {
			final List<ButtonType> buttons = new ArrayList<>();
			buttons.add(ButtonType.YES);
			buttons.add(ButtonType.NO);
			final Alert alert = stageManager.makeAlert(Alert.AlertType.CONFIRMATION, buttons,
					"plugin.pluginMenu.alerts.confirmRemovingPlugin.header",
					"plugin.pluginMenu.alerts.confirmRemovingPlugin.content", pluginName);
			alert.initOwner(this);
			alert.showAndWait().ifPresent(response -> {
				if (response == ButtonType.YES) {
					getProBJarPluginManager().deletePlugin(pluginId);
				}
			});
		});
		return new ContextMenu(restartItem, removeMenuItem);
	}

	private ProBPluginManager.ProBJarPluginManager getProBJarPluginManager() {
		return proBPluginManager.getPluginManager();
	}
}
