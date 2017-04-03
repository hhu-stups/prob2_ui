package de.prob2.ui.project.machines;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob.animator.command.GetPreferenceCommand;
import de.prob.scripting.Api;
import de.prob.statespace.StateSpace;
import de.prob2.ui.beditor.BEditorStage;
import de.prob2.ui.internal.ProB2Module;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.preferences.ProBPreferences;
import de.prob2.ui.prob2fx.CurrentProject;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.stage.FileChooser;

@Singleton
public class MachinesTab extends Tab {
	@FXML
	private TableView<Machine> machinesTable;
	@FXML
	private TableColumn<Machine, String> nameColumn;
	@FXML
	private TableColumn<Machine, Path> machineColumn;
	@FXML
	private TableColumn<Machine, String> descriptionColumn;

	private final CurrentProject currentProject;
	private final StageManager stageManager;
	private final Injector injector;
	private final Api api;

	private static final Logger LOGGER = LoggerFactory.getLogger(MachinesTab.class);

	@Inject
	private MachinesTab(final StageManager stageManager, final CurrentProject currentProject, final Injector injector,
			final Api api) {
		this.stageManager = stageManager;
		this.currentProject = currentProject;
		this.injector = injector;
		this.api = api;
		stageManager.loadFXML(this, "machines_tab.fxml");
	}

	@FXML
	public void initialize() {
		nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
		machineColumn.setCellValueFactory(cellData -> new SimpleObjectProperty<Path>(cellData.getValue().getPath()));
		descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));

		machinesTable.setRowFactory(tableView -> {
			final TableRow<Machine> row = new TableRow<>();

			final MenuItem removeMachineMenuItem = new MenuItem("Remove Machine");
			removeMachineMenuItem.setOnAction(event -> currentProject.removeMachine(row.getItem()));
			removeMachineMenuItem.disableProperty().bind(row.emptyProperty());

			final MenuItem editFileMenuItem = new MenuItem("Edit File");
			editFileMenuItem.setOnAction(event -> this.showEditorStage(row.getItem()));
			editFileMenuItem.disableProperty().bind(row.emptyProperty());

			final MenuItem editExternalMenuItem = new MenuItem("Edit File in External Editor");
			editExternalMenuItem.setOnAction(event -> this.showExternalEditor(row.getItem()));
			editExternalMenuItem.disableProperty().bind(row.emptyProperty());

			row.setContextMenu(new ContextMenu(removeMachineMenuItem, editFileMenuItem, editExternalMenuItem));

			row.setOnMouseClicked(event -> {
				if (event.getButton().equals(MouseButton.PRIMARY) && event.getClickCount() == 2) {
					injector.getInstance(EditMachinesDialog.class).editAndShow(row.getItem())
							.ifPresent(result -> currentProject.updateMachine(row.getItem(), result));
				}
			});

			return row;
		});
		machinesTable.itemsProperty().bind(currentProject.machinesProperty());
	}

	@FXML
	void addMachine() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Add Machine");
		fileChooser.getExtensionFilters()
				.add(new FileChooser.ExtensionFilter("Classical B Files", "*.mch", "*.ref", "*.imp"));

		File machineFile = fileChooser.showOpenDialog(stageManager.getCurrent());
		if (machineFile == null) {
			return;
		}
		Path projectLocation = currentProject.getLocation().toPath();
		Path absolute = machineFile.toPath();
		Path relative = projectLocation.relativize(absolute);
		if (currentProject.getMachines().contains(new Machine("", "", relative))) {
			stageManager.makeAlert(Alert.AlertType.ERROR, "The machine \"" + machineFile
			+ "\" already exists in the current project.").showAndWait();
			return;
		}
		injector.getInstance(AddMachinesDialog.class).showAndWait(relative);
	}

	private void showEditorStage(Machine machine) {
		final BEditorStage editorStage = injector.getInstance(BEditorStage.class);
		final Path path = currentProject.getLocation().toPath().resolve(machine.getPath());
		final String text;
		try {
			text = Files.lines(path).collect(Collectors.joining(System.lineSeparator()));
		} catch (IOException | UncheckedIOException e) {
			LOGGER.error("Could not read file " + path, e);
			stageManager.makeAlert(Alert.AlertType.ERROR, "Could not read file:\n" + path + "\n" + e).showAndWait();
			return;
		}
		editorStage.getEngine().getLoadWorker().stateProperty().addListener((ov, oldState, newState) -> {
			if (newState == Worker.State.SUCCEEDED) {
				editorStage.setTextEditor(text, path);
			}
		});
		editorStage.setTitle(machine.getFileName());
		editorStage.show();
	}

	private void showExternalEditor(Machine machine) {
		final StateSpace stateSpace = ProBPreferences.getEmptyStateSpace(api);
		final GetPreferenceCommand cmd = new GetPreferenceCommand("EDITOR_GUI");
		stateSpace.execute(cmd);
		final File editor = new File(cmd.getValue());
		final Path machinePath = currentProject.getLocation().toPath().resolve(machine.getPath());
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
			stageManager.makeAlert(Alert.AlertType.ERROR, "Failed to start external editor:\n" + e).showAndWait();
		}
	}
}
