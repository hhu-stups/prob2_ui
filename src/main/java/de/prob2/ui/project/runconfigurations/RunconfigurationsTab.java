package de.prob2.ui.project.runconfigurations;

import com.google.inject.Inject;
import com.google.inject.Injector;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import javafx.fxml.FXML;
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

	private final CurrentProject currentProject;
	private final Injector injector;

	@Inject
	private RunconfigurationsTab(final StageManager stageManager, final CurrentProject currentProject,
			final Injector injector) {
		this.currentProject = currentProject;
		this.injector = injector;
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
				Runconfiguration selectedItem = runconfigurationsListView.getSelectionModel().getSelectedItem();
				if (selectedItem != null) {
					currentProject.startAnimation(selectedItem);
				}
			}
		});
	}

	@FXML
	void addRunconfiguration() {
		injector.getInstance(RunconfigurationsDialog.class).showAndWait()
				.ifPresent(currentProject::addRunconfiguration);
	}
}
