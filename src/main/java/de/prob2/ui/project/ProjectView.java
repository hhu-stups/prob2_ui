package de.prob2.ui.project;

import java.io.File;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;

@Singleton
public final class ProjectView extends AnchorPane {
	private static final Logger LOGGER = LoggerFactory.getLogger(ProjectView.class);

	@FXML
	private TableView<Machine> machinesTable;
	@FXML
	private TableColumn<Machine, String> name;
	@FXML
	private TableColumn<Machine, Path> machine;
	@FXML
	private TableColumn<Machine, String> description;
	@FXML
	private Button addMachineButton;

	private final CurrentProject currentProject;
	private final MachineLoader machineLoader;
	private final StageManager stageManager;

	@Inject
	private ProjectView(final StageManager stageManager, final CurrentProject currentProject,
			final MachineLoader machineLoader) {
		this.stageManager = stageManager;
		this.currentProject = currentProject;
		this.machineLoader = machineLoader;
		stageManager.loadFXML(this, "project_view.fxml");
	}

	@FXML
	public void initialize() {
		name.setCellValueFactory(new PropertyValueFactory<>("name"));
		machine.setCellValueFactory(cellData -> new SimpleObjectProperty<Path>(cellData.getValue().getPath()));
		description.setCellValueFactory(new PropertyValueFactory<>("description"));

		machinesTable.setRowFactory(tableView -> {
			final TableRow<Machine> row = new TableRow<>();
			final ContextMenu contextMenu = new ContextMenu();
			final MenuItem editMenuItem = new MenuItem("Edit Machine");
			MachineStage machineStage = new MachineStage(stageManager, currentProject);
			editMenuItem.setOnAction(event -> machineStage.editMachine(row.getItem()));
			contextMenu.getItems().add(editMenuItem);
			row.setOnMouseClicked(event -> {
				Machine machine = row.getItem();
				if (event.getButton() == MouseButton.SECONDARY) {
					if (row.isEmpty()) {
						contextMenu.getItems().get(0).setDisable(true);
					} else {
						contextMenu.getItems().get(0).setDisable(false);
					}
					contextMenu.show(row, event.getScreenX(), event.getScreenY());
				} else if (event.getButton().equals(MouseButton.PRIMARY) && event.getClickCount() == 2) {
					
					try {
						machineLoader.loadAsync(machine);
					} catch (Exception e) {
						LOGGER.error("Loading machine \"" + machine.getName() + "\" failed", e);
						Platform.runLater(
								() -> stageManager
										.makeAlert(Alert.AlertType.ERROR,
												"Could not open machine \"" + machine.getName() + "\":\n" + e)
										.showAndWait());
					}
				}
			});
			return row;
		});

		machinesTable.itemsProperty().bind(currentProject.machinesProperty());
		addMachineButton.disableProperty().bind(currentProject.existsProperty().not());
	}

	@FXML
	void addMachine(ActionEvent event) {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Add Machine");
		fileChooser.getExtensionFilters()
				.add(new FileChooser.ExtensionFilter("Classical B Files", "*.mch", "*.ref", "*.imp"));

		final File selectedFile = fileChooser.showOpenDialog(stageManager.getCurrent());
		if (selectedFile == null) {
			return;
		}

		MachineStage machineStage = new MachineStage(stageManager, currentProject);
		machineStage.addNewMachine(selectedFile);
	}
}
