package de.prob2.ui.machines;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.Machine;
import de.prob2.ui.project.MachineLoader;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;

@Singleton
public final class MachinesView extends AnchorPane {
	private static final Logger LOGGER = LoggerFactory.getLogger(MachinesView.class);
	
	@FXML
	private TableView<Machine> machinesTable;
	@FXML
	private TableColumn<Machine, String> name;
	@FXML
	private TableColumn<Machine, File> machine;
	@FXML
	private TableColumn<Machine, String> description;
	private final CurrentProject currentProject;
	private final MachineLoader machineLoader;
	private final StageManager stageManager;

	@Inject
	private MachinesView(final StageManager stageManager, final CurrentProject currentProject,
			final MachineLoader machineLoader) {
		this.stageManager = stageManager;
		this.currentProject = currentProject;
		this.machineLoader = machineLoader;
		stageManager.loadFXML(this, "machines_view.fxml");
	}

	@FXML
	public void initialize() {
		name.setCellValueFactory(new PropertyValueFactory<>("name"));
		machine.setCellValueFactory(cellData -> new SimpleObjectProperty<File>(cellData.getValue().getLocation()));
		description.setCellValueFactory(new PropertyValueFactory<>("description"));
		
		machinesTable.setRowFactory(tableView -> {
			final TableRow<Machine> row = new TableRow<>();	
			row.setOnMouseClicked(event -> {
				Machine machine = row.getItem();
				try {
					machineLoader.load(machine);
				} catch (Exception e) {
					LOGGER.error("Loading machine \"" + machine.getName() + "\" failed", e);
					Platform.runLater(() -> stageManager.makeAlert(Alert.AlertType.ERROR,
						"Could not open machine \"" + machine.getName() + "\":\n" + e).showAndWait());
				}
			});
			return row;
		});
		
		machinesTable.itemsProperty().bind(currentProject.machinesProperty());
	}
}
