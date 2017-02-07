package de.prob2.ui.project;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Pair;

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
	private ListView<Preference> preferencesListView;
	@FXML
	private TabPane projectTabPane;
	@FXML
	private Button newProjectButton;
	@FXML
	private Label runconfigsPlaceholder;
	@FXML
	private Button addRunconfigButton;
	@FXML
	private ListView<Runconfiguration> runconfigurationsListView;
	

	private final CurrentProject currentProject;
	private final MachineLoader machineLoader;
	private final StageManager stageManager;
	private final Injector injector;

	@Inject
	private ProjectView(final StageManager stageManager, final CurrentProject currentProject,
			final MachineLoader machineLoader, final Injector injector) {
		this.stageManager = stageManager;
		this.currentProject = currentProject;
		this.machineLoader = machineLoader;
		this.injector = injector;
		stageManager.loadFXML(this, "project_view.fxml");
	}

	@FXML
	public void initialize() {
		projectTabPane.visibleProperty().bind(currentProject.existsProperty());
		newProjectButton.visibleProperty().bind(projectTabPane.visibleProperty().not());

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
		preferencesListView.itemsProperty().bind(currentProject.preferencesProperty());
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
	}

	@FXML
	private void createNewProject() {
		final Stage newProjectStage = injector.getInstance(NewProjectStage.class);
		newProjectStage.showAndWait();
		newProjectStage.toFront();
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

	@FXML
	void addPreference(ActionEvent event) {
		AddProBPreferencesStage addProBPreferencesStage = new AddProBPreferencesStage(stageManager, currentProject);
		addProBPreferencesStage.showStage();
	}

	@FXML
	void addRunconfiguration(ActionEvent event) {
		Dialog<Pair<Machine, Preference>> dialog = new Dialog<>();
		dialog.setTitle("New Runconfiguration");
		dialog.initStyle(StageStyle.UTILITY);
		dialog.getDialogPane().getStylesheets().add("prob.css");
		ButtonType addButtonType = new ButtonType("Add", ButtonData.OK_DONE);
		dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);
		GridPane grid = new GridPane();
		grid.setHgap(10);
		grid.setVgap(10);
		ChoiceBox<Machine> machinesBox = new ChoiceBox<>(currentProject.machinesProperty());
		grid.add(new Label("Machine:"), 0, 0);
		grid.add(machinesBox, 1, 0);
		ChoiceBox<Preference> prefsBox = new ChoiceBox<>(currentProject.preferencesProperty());
		grid.add(new Label("Preference:"), 0, 1);
		grid.add(prefsBox, 1, 1);
		
		dialog.getDialogPane().setContent(grid);
		dialog.setResultConverter(dialogButton -> {
		    if (dialogButton == addButtonType) {
		        return new Pair<>(machinesBox.getValue(), prefsBox.getValue());
		    }
		    return null;
		});
		Optional<Pair<Machine, Preference>> result = dialog.showAndWait();
		result.ifPresent(runconfiguration -> currentProject.addRunconfiguration(runconfiguration));
	}
}
