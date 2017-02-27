package de.prob2.ui.project;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob.animator.command.GetPreferenceCommand;
import de.prob.scripting.Api;
import de.prob.statespace.StateSpace;
import de.prob2.ui.animations.AnimationsView;
import de.prob2.ui.beditor.BEditorStage;
import de.prob2.ui.internal.ProB2Module;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.preferences.PreferencesDialog;
import de.prob2.ui.preferences.ProBPreferences;
import de.prob2.ui.prob2fx.CurrentProject;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
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

	private static final Logger LOGGER = LoggerFactory.getLogger(AnimationsView.class);

	private final CurrentProject currentProject;
	private final MachineLoader machineLoader;
	private final StageManager stageManager;
	private final Injector injector;
	private final Api api;

	@Inject
	private ProjectView(final StageManager stageManager, final CurrentProject currentProject,
			final MachineLoader machineLoader, final Injector injector, final Api api) {
		this.stageManager = stageManager;
		this.currentProject = currentProject;
		this.machineLoader = machineLoader;
		this.injector = injector;
		this.api = api;
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
			final MenuItem editMenuItem = new MenuItem("Edit Machine");
			MachineStage machineStage = new MachineStage(stageManager, currentProject);
			editMenuItem.setOnAction(event -> machineStage.editMachine(row.getItem()));
			editMenuItem.disableProperty().bind(row.emptyProperty());

			final MenuItem editFileMenuItem = new MenuItem("Edit File");
			editFileMenuItem.setOnAction(event -> this.showEditorStage(row.getItem()));
			editFileMenuItem.disableProperty().bind(row.emptyProperty());

			final MenuItem editExternalMenuItem = new MenuItem("Edit File in External Editor");
			editExternalMenuItem.setOnAction(event -> {
				final StateSpace stateSpace = ProBPreferences.getEmptyStateSpace(api);
				final GetPreferenceCommand cmd = new GetPreferenceCommand("EDITOR_GUI");
				stateSpace.execute(cmd);
				final File editor = new File(cmd.getValue());
				Path machinePath = row.getItem().getPath();
				final String[] cmdline;
				if (ProB2Module.IS_MAC && editor.isDirectory()) {
					// On Mac, use the open tool to start app bundles
					cmdline = new String[] { "/usr/bin/open", "-a", editor.getAbsolutePath(), machinePath.toString() };
				} else {
					// Run normal executables directly
					cmdline = new String[] { editor.getAbsolutePath(), machinePath.toString() };
				}
				final ProcessBuilder processBuilder = new ProcessBuilder(cmdline);
				try {
					processBuilder.start();
				} catch (IOException e) {
					LOGGER.error("Failed to start external editor", e);
					stageManager.makeAlert(Alert.AlertType.ERROR, "Failed to start external editor:\n" + e)
							.showAndWait();
				}
			});
			editExternalMenuItem.disableProperty().bind(row.emptyProperty());

			row.setContextMenu(new ContextMenu(editMenuItem, editFileMenuItem, editExternalMenuItem));

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

	private void showEditorStage(Machine machine) {
		final BEditorStage editorStage = injector.getInstance(BEditorStage.class);
		final Path path = Paths.get(currentProject.get().getLocation().getPath(), machine.getPath().toString());
		final String text;
		try {
			text = Files.lines(path).collect(Collectors.joining(System.lineSeparator()));
		} catch (IOException | UncheckedIOException e) {
			LOGGER.error("Could not read file " + path, e);
			stageManager.makeAlert(Alert.AlertType.ERROR, "Could not read file:\n" + path + "\n" + e).showAndWait();
			return;
		}
		editorStage.getEngine().getLoadWorker().stateProperty().addListener((ov, oldState, newState) -> {
			if (newState == Worker.State.SUCCEEDED && !editorStage.getLoaded()) {
				editorStage.setTextEditor(text, path);
			}
		});
		editorStage.setTitle(machine.getFileName());
		editorStage.show();
	}
}
