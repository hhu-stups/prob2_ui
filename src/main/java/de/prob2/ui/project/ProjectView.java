package de.prob2.ui.project;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.preferences.PreferencesDialog;
import de.prob2.ui.prob2fx.CurrentProject;

import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
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
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Pair;

@Singleton
public final class ProjectView extends AnchorPane {
	@FXML
	private Label projectNameLabel;
	@FXML
	private Text projectDescriptionText;
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

		projectNameLabel.textProperty().bind(currentProject.nameProperty());
		projectDescriptionText.textProperty().bind(currentProject.descriptionProperty());
		this.projectTabPane.widthProperty().addListener((observableValue, oldValue, newValue) -> {
			if (newValue == null) {
				projectDescriptionText.setWrappingWidth(0);
				return;
			}
			projectDescriptionText.setWrappingWidth(newValue.doubleValue() - 20);
		});

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
				if (event.getButton() == MouseButton.SECONDARY) {
					if (row.isEmpty()) {
						contextMenu.getItems().get(0).setDisable(true);
					} else {
						contextMenu.getItems().get(0).setDisable(false);
					}
					contextMenu.show(row, event.getScreenX(), event.getScreenY());
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
		runconfigurationsListView.setOnMouseClicked(event -> {
			if (event.getButton().equals(MouseButton.PRIMARY) && event.getClickCount() == 2) {
				Runconfiguration runconfig = runconfigurationsListView.getSelectionModel().getSelectedItem();
				Machine m = currentProject.getMachine(runconfig.getMachine());
				Map<String, String> pref = new HashMap<>();
				if (!"default".equals(runconfig.getPreference())) {
					pref = currentProject.getPreferencAsMap(runconfig.getPreference());
				}
				if (m != null && pref != null) {
					machineLoader.loadAsync(m, pref);
				}
			}
		});
	}

	@FXML
	private void createNewProject() {
		final Stage newProjectStage = injector.getInstance(NewProjectStage.class);
		newProjectStage.showAndWait();
		newProjectStage.toFront();
	}

	@FXML
	void addMachine() {
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
	void addPreference() {
		injector.getInstance(PreferencesDialog.class).showAndWait().ifPresent(currentProject::addPreference);
	}

	@FXML
	void addRunconfiguration() {
		Dialog<Pair<Machine, Preference>> dialog = new Dialog<>();
		dialog.setTitle("New Runconfiguration");
		dialog.initStyle(StageStyle.UTILITY);
		dialog.getDialogPane().getStylesheets().add("prob.css");
		ButtonType addButtonType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
		dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);
		GridPane grid = new GridPane();
		grid.setHgap(10);
		grid.setVgap(10);
		ChoiceBox<Machine> machinesBox = new ChoiceBox<>(currentProject.machinesProperty());
		grid.add(new Label("Machine:"), 0, 0);
		grid.add(machinesBox, 1, 0);
		ChoiceBox<Preference> prefsBox = new ChoiceBox<>();
		prefsBox.getItems().add(new Preference("default", null));
		prefsBox.getItems().addAll(currentProject.getPreferences());
		grid.add(new Label("Preference:"), 0, 1);
		grid.add(prefsBox, 1, 1);

		dialog.getDialogPane().setContent(grid);
		dialog.getDialogPane().lookupButton(addButtonType).disableProperty()
				.bind(machinesBox.valueProperty().isNotNull().and(prefsBox.valueProperty().isNotNull()).not());
		dialog.setResultConverter(dialogButton -> {
			if (dialogButton == addButtonType) {
				return new Pair<>(machinesBox.getValue(), prefsBox.getValue());
			}
			return null;
		});
		Optional<Pair<Machine, Preference>> result = dialog.showAndWait();
		result.ifPresent(currentProject::addRunconfiguration);
	}
}
