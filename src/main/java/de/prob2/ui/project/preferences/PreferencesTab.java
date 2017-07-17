package de.prob2.ui.project.preferences;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob2.ui.ProB2;
import de.prob2.ui.helpsystem.HelpButton;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.layout.FontSize;
import de.prob2.ui.prob2fx.CurrentProject;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.input.MouseButton;

import java.net.URISyntaxException;

@Singleton
public class PreferencesTab extends Tab {

	@FXML
	private ListView<Preference> preferencesListView;
	@FXML
	private SplitPane splitPane;
	@FXML
	private Button addPreferenceButton;
	@FXML
	private HelpButton helpButton;

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
	public void initialize() throws URISyntaxException {
		helpButton.setPathToHelp(ProB2.class.getClassLoader().getResource("help/HelpMain.html").toURI().toString());
		preferencesListView.itemsProperty().bind(currentProject.preferencesProperty());
		preferencesListView.setCellFactory(listView -> initListCell());

		FontSize fontsize = injector.getInstance(FontSize.class);
		((FontAwesomeIconView) (addPreferenceButton.getGraphic())).glyphSizeProperty().bind(fontsize.multiply(2.0));
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
		removePreferenceMenuItem.setOnAction(event -> {
			if (splitPane.getItems().size() >= 2
					&& ((PreferenceView) splitPane.getItems().get(0)).getPreference() == cell.getItem()) {
				closePreferenceView();
			}
			currentProject.removePreference(cell.getItem());
		});
		removePreferenceMenuItem.disableProperty().bind(cell.emptyProperty());

		final MenuItem editMenuItem = new MenuItem("Edit");
		editMenuItem.setOnAction(event -> {
			PreferencesDialog prefDialog = injector.getInstance(PreferencesDialog.class);
			Preference pref = cell.getItem();
			prefDialog.setPreference(pref);
			prefDialog.showAndWait().ifPresent(result -> {
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
		});

		return cell;
	}

	@FXML
	void addPreference() {
		injector.getInstance(PreferencesDialog.class).showAndWait().ifPresent(currentProject::addPreference);
	}

	public void showPreferenceView(Preference pref) {
		if (splitPane.getItems().size() >= 2) {
			splitPane.getItems().remove(0);
		}
		splitPane.getItems().add(0, new PreferenceView(pref, stageManager, injector));
	}

	public void closePreferenceView() {
		splitPane.getItems().remove(0);
	}
}
