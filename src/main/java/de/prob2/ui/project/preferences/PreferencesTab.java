package de.prob2.ui.project.preferences;

import java.util.ResourceBundle;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob2.ui.helpsystem.HelpButton;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.machines.Machine;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.input.MouseButton;

@FXMLInjected
@Singleton
public class PreferencesTab extends Tab {

	@FXML
	private ListView<Preference> preferencesListView;
	@FXML
	private SplitPane splitPane;
	@FXML
	private HelpButton helpButton;

	private final ResourceBundle bundle;
	private final CurrentProject currentProject;
	private final Injector injector;
	private final StageManager stageManager;

	@Inject
	private PreferencesTab(final StageManager stageManager, final ResourceBundle bundle, final CurrentProject currentProject, final Injector injector) {
		this.stageManager = stageManager;
		this.bundle = bundle;
		this.currentProject = currentProject;
		this.injector = injector;
		stageManager.loadFXML(this, "preferences_tab.fxml");
	}

	@FXML
	public void initialize() {
		helpButton.setHelpContent("project", "Preferences");
		preferencesListView.itemsProperty().bind(currentProject.preferencesProperty());
		preferencesListView.setCellFactory(listView -> initListCell());

		currentProject.preferencesProperty().addListener((observable, from, to) -> {
			Node node = splitPane.getItems().get(0);
			if (node instanceof PreferenceView && !to.contains(((PreferenceView) node).getPreference())) {
				closePreferenceView();
			}
		});
	}

	private ListCell<Preference> initListCell() {
		ListCell<Preference> cell = new ListCell<Preference>() {
			@Override
			public void updateItem(Preference preference, boolean empty) {
				super.updateItem(preference, empty);
				if (empty) {
					setText(null);
					setGraphic(null);
				} else {
					setText(preference.getName());
					setGraphic(null);
				}
			}
		};

		final MenuItem removePreferenceMenuItem = new MenuItem(bundle.getString("project.preferences.preferencesTab.contextMenu.items.remove"));
		removePreferenceMenuItem.setOnAction(event -> currentProject.removePreference(cell.getItem()));
		removePreferenceMenuItem.disableProperty().bind(cell.emptyProperty());

		final MenuItem editMenuItem = new MenuItem(bundle.getString("project.preferences.preferencesTab.contextMenu.items.edit"));
		editMenuItem.setOnAction(event -> {
			PreferencesDialog prefDialog = injector.getInstance(PreferencesDialog.class);
			prefDialog.initOwner(this.getTabPane().getScene().getWindow());
			Preference pref = cell.getItem();
			final String oldName = pref.getName();
			prefDialog.setPreference(pref);
			prefDialog.showAndWait().ifPresent(result -> {
				if (!oldName.equals(pref.getName())) {
					// If preference was renamed, update lastUsedPreferenceName of machines that use the old name.
					for (final Machine machine : currentProject.getMachines()) {
						if (oldName.equals(machine.getLastUsedPreferenceName())) {
							machine.setLastUsedPreferenceName(pref.getName());
						}
					}
				}
				preferencesListView.refresh();
				showPreferenceView(pref);
			});
		});
		editMenuItem.disableProperty().bind(cell.emptyProperty());

		cell.setContextMenu(new ContextMenu(editMenuItem, removePreferenceMenuItem));

		cell.setOnMouseClicked(event -> {
			if (event.getButton().equals(MouseButton.PRIMARY) && event.getClickCount() == 2) {
				showPreferenceView(cell.getItem());
			}
			if (splitPane.getItems().get(0) instanceof PreferenceView) {
				PreferenceView prefView = (PreferenceView) splitPane.getItems().get(0);
				preferencesListView.getSelectionModel().select(prefView.getPreference());
			} else {
				preferencesListView.getSelectionModel().clearSelection();
			}
		});
		// makes sure that the selected item is always the preference currently
		// displayed in Preference View. Otherwise this preference would be
		// unselected as long as a mouse button is pressed
		cell.setOnMousePressed(event -> {
			if (splitPane.getItems().get(0) instanceof PreferenceView) {
				PreferenceView prefView = (PreferenceView) splitPane.getItems().get(0);
				preferencesListView.getSelectionModel().select(prefView.getPreference());
			} else {
				preferencesListView.getSelectionModel().clearSelection();
			}
		});

		return cell;
	}

	@FXML
	void addPreference() {
		final PreferencesDialog prefDialog = injector.getInstance(PreferencesDialog.class);
		prefDialog.initOwner(this.getTabPane().getScene().getWindow());
		prefDialog.showAndWait().ifPresent(currentProject::addPreference);
	}

	public void showPreferenceView(Preference pref) {
		if (splitPane.getItems().size() >= 2) {
			splitPane.getItems().remove(0);
		}
		splitPane.getItems().add(0, new PreferenceView(pref, stageManager, injector));
	}

	public void closePreferenceView() {
		if (splitPane.getItems().get(0) instanceof PreferenceView) {
			splitPane.getItems().remove(0);
		}
		preferencesListView.getSelectionModel().clearSelection();
	}
}
