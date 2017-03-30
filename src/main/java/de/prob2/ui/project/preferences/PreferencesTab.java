package de.prob2.ui.project.preferences;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import javafx.fxml.FXML;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.input.MouseButton;

@Singleton
public class PreferencesTab extends Tab {

	@FXML
	private ListView<Preference> preferencesListView;
	@FXML
	private SplitPane splitPane;

	private final CurrentProject currentProject;
	private final Injector injector;
	private final StageManager stageManager;

	@Inject
	private PreferencesTab(final StageManager stageManager, final CurrentProject currentProject,
			final Injector injector) {
		this.stageManager = stageManager;
		this.currentProject = currentProject;
		this.injector = injector;
		stageManager.loadFXML(this, "preferences_tab.fxml");
	}

	@FXML
	public void initialize() {
		preferencesListView.itemsProperty().bind(currentProject.preferencesProperty());
		preferencesListView.setCellFactory(listView -> initListCell());
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

		final MenuItem removePreferenceMenuItem = new MenuItem("Remove Preference");
		removePreferenceMenuItem.setOnAction(event -> currentProject.removePreference(cell.getItem()));
		removePreferenceMenuItem.disableProperty().bind(cell.emptyProperty());

		cell.setContextMenu(new ContextMenu(removePreferenceMenuItem));

		cell.setOnMouseClicked(event -> {
			if (event.getButton().equals(MouseButton.PRIMARY) && event.getClickCount() == 2) {
				if (splitPane.getItems().size() >= 2) {
					splitPane.getItems().remove(0);
				}
				splitPane.getItems().add(0, new PreferenceView(cell.getItem(), stageManager, injector));
			}
		});

		return cell;
	}

	@FXML
	void addPreference() {
		injector.getInstance(PreferencesDialog.class).showAndWait().ifPresent(currentProject::addPreference);
	}

	public void closePreferenceView() {
		splitPane.getItems().remove(0);
	}
}
