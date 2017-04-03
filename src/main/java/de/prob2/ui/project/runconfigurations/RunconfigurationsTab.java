package de.prob2.ui.project.runconfigurations;

import java.util.HashMap;
import java.util.Map;

import com.google.inject.Inject;
import com.google.inject.Injector;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.MachineLoader;
import de.prob2.ui.project.machines.Machine;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.input.MouseButton;

public class RunconfigurationsTab extends Tab {

	@FXML
	private Label runconfigsPlaceholder;
	@FXML
	private Button addRunconfigButton;
	@FXML
	private ListView<Runconfiguration> runconfigurationsListView;

	private final StageManager stageManager;
	private final CurrentProject currentProject;
	private final Injector injector;
	private final MachineLoader machineLoader;
	
	@Inject
	private RunconfigurationsTab(final StageManager stageManager, final CurrentProject currentProject,
			final Injector injector, final MachineLoader machineLoader) {
		this.stageManager = stageManager;
		this.currentProject = currentProject;
		this.injector = injector;
		this.machineLoader = machineLoader;
		stageManager.loadFXML(this, "runconfigurations_tab.fxml");
	}
	
	@FXML
	public void initialize() {
		runconfigsPlaceholder.setText("Add machines first");
		currentProject.machinesProperty().emptyProperty().addListener((observable, from, to) -> {
			if (to) {
				runconfigsPlaceholder.setText("Add machines first");
				addRunconfigButton.setDisable(true);
			} else {
				runconfigsPlaceholder.setText("No Runconfigurations");
				addRunconfigButton.setDisable(false);
			}
		});
		runconfigurationsListView.itemsProperty().bind(currentProject.runconfigurationsProperty());
		runconfigurationsListView.setCellFactory(listView -> {
			ListCell<Runconfiguration> cell = new ListCell<Runconfiguration>() {
				@Override
				public void updateItem(Runconfiguration runconfiguration, boolean empty) {
					super.updateItem(runconfiguration, empty);
					if (empty) {
						setText(null);
						setGraphic(null);
					} else {
						setText(runconfiguration.toString());
						setGraphic(null);
					}
				}
			};

			final MenuItem removeRunconfigMenuItem = new MenuItem("Remove Runconfiguration");
			removeRunconfigMenuItem.setOnAction(event -> currentProject.removeRunconfiguration(cell.getItem()));
			removeRunconfigMenuItem.disableProperty().bind(cell.emptyProperty());

			cell.setContextMenu(new ContextMenu(removeRunconfigMenuItem));

			return cell;
		});
		runconfigurationsListView.setOnMouseClicked(event -> {
			if (event.getButton().equals(MouseButton.PRIMARY) && event.getClickCount() == 2) {
				startAnimation(runconfigurationsListView.getSelectionModel().getSelectedItem());
			}
		});
	}
	
	@FXML
	void addRunconfiguration() {
		injector.getInstance(RunconfigurationsDialog.class).showAndWait()
				.ifPresent(currentProject::addRunconfiguration);
	}

	private void startAnimation(Runconfiguration runconfiguration) {
		Machine m = currentProject.getMachine(runconfiguration.getMachine());
		Map<String, String> pref = new HashMap<>();
		if (!"default".equals(runconfiguration.getPreference())) {
			pref = currentProject.getPreferencAsMap(runconfiguration.getPreference());
		}
		if (m != null && pref != null) {
			machineLoader.loadAsync(m, pref);
		} else {
			stageManager.makeAlert(Alert.AlertType.ERROR, "Could not load machine \"" + runconfiguration.getMachine()
					+ "\" with preferences: \"" + runconfiguration.getPreference() + "\"").showAndWait();
		}
	}
}
