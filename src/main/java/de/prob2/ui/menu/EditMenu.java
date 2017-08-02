package de.prob2.ui.menu;

import com.google.inject.Inject;
import com.google.inject.Injector;
import de.prob.animator.command.GetPreferenceCommand;
import de.prob.statespace.StateSpace;
import de.prob2.ui.beditor.BEditorStage;
import de.prob2.ui.internal.ProB2Module;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.preferences.GlobalPreferences;
import de.prob2.ui.preferences.PreferencesStage;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.MachineLoader;
import de.prob2.ui.project.machines.Machine;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

public class EditMenu extends Menu {
	private static final Logger LOGGER = LoggerFactory.getLogger(EditMenu.class);

	@FXML
	private MenuItem editCurrentMachineItem;
	@FXML
	private MenuItem editCurrentMachineInExternalEditorItem;
	@FXML
	private MenuItem preferencesItem;

	private final CurrentProject currentProject;
	private final Injector injector;
	private final MachineLoader machineLoader;
	private final GlobalPreferences globalPreferences;
	private final StageManager stageManager;

	@Inject
	private EditMenu(final StageManager stageManager, final CurrentProject currentProject, final Injector injector,
			final MachineLoader machineLoader, final GlobalPreferences globalPreferences) {
		this.currentProject = currentProject;
		this.injector = injector;
		this.machineLoader = machineLoader;
		this.globalPreferences = globalPreferences;
		this.stageManager = stageManager;
		stageManager.loadFXML(this, "editMenu.fxml");
	}

	@FXML
	public void initialize() {
		this.editCurrentMachineItem.disableProperty().bind(currentProject.currentMachineProperty().isNull());
		this.editCurrentMachineInExternalEditorItem.disableProperty()
				.bind(currentProject.currentMachineProperty().isNull());
	}

	@FXML
	private void handleEditCurrentMachine() {
		showEditorStage(currentProject.getCurrentMachine());
	}

	@FXML
	private void handleEditCurrentMachineInExternalEditor() {
		showExternalEditor(currentProject.getCurrentMachine());
	}

	@FXML
	private void handlePreferences() {
		final Stage preferencesStage = injector.getInstance(PreferencesStage.class);
		preferencesStage.show();
		preferencesStage.toFront();
	}

	MenuItem getPreferencesItem() {
		return preferencesItem;
	}

	public void showEditorStage(Machine machine) {
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

	public void showExternalEditor(Machine machine) {
		final StateSpace stateSpace = machineLoader.getEmptyStateSpace(globalPreferences);
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
