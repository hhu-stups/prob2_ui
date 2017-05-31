package de.prob2.ui.project.machines;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

import de.prob2.ui.helpsystem.HelpSystemStage;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import de.prob.animator.command.GetPreferenceCommand;
import de.prob.scripting.Api;
import de.prob.statespace.StateSpace;
import de.prob2.ui.beditor.BEditorStage;
import de.prob2.ui.internal.ProB2Module;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.layout.FontSize;
import de.prob2.ui.preferences.GlobalPreferences;
import de.prob2.ui.preferences.ProBPreferences;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.runconfigurations.Runconfiguration;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;

@Singleton
public class MachinesTab extends Tab {
	@FXML
	private VBox machinesVBox;
	@FXML
	private StackPane noMachinesStack;
	@FXML
	private AnchorPane descriptionView;
	@FXML
	private Label descriptionViewTitelLabel;
	@FXML
	private Text descriptionText;
	@FXML
	private SplitPane splitPane;
	@FXML
	private Button addMachineButton;
	@FXML
	private Button closeDescriptionButton;

	private final CurrentProject currentProject;
	private final StageManager stageManager;
	private final Injector injector;
	private final Api api;
	private final GlobalPreferences globalPreferences;

	private static final Logger LOGGER = LoggerFactory.getLogger(MachinesTab.class);

	@Inject
	private MachinesTab(final StageManager stageManager, final CurrentProject currentProject, final Injector injector,
			final Api api, final GlobalPreferences globalPreferences) {
		this.stageManager = stageManager;
		this.currentProject = currentProject;
		this.injector = injector;
		this.api = api;
		this.globalPreferences = globalPreferences;
		stageManager.loadFXML(this, "machines_tab.fxml");
	}

	@FXML
	public void initialize() {
		noMachinesStack.managedProperty().bind(currentProject.machinesProperty().emptyProperty());
		noMachinesStack.visibleProperty().bind(currentProject.machinesProperty().emptyProperty());

		splitPane.getItems().remove(descriptionView);

		currentProject.machinesProperty().addListener((observable, from, to) -> {
			machinesVBox.getChildren().clear();
			for (Machine machine : to) {
				MachinesItem machinesItem = new MachinesItem(machine, stageManager);
				machinesVBox.getChildren().add(machinesItem);

				final MenuItem editMachineMenuItem = new MenuItem("Edit Machine");
				editMachineMenuItem.setOnAction(event -> injector.getInstance(EditMachinesDialog.class)
						.editAndShow(machine).ifPresent(result -> machinesItem.refresh()));

				final MenuItem removeMachineMenuItem = new MenuItem("Remove Machine");
				removeMachineMenuItem.setOnAction(event -> currentProject.removeMachine(machine));

				final MenuItem editFileMenuItem = new MenuItem("Edit File");
				editFileMenuItem.setOnAction(event -> this.showEditorStage(machine));

				final MenuItem editExternalMenuItem = new MenuItem("Edit File in External Editor");
				editExternalMenuItem.setOnAction(event -> this.showExternalEditor(machine));

				final Menu startAnimationMenu = new Menu("Start Animation...");

				ContextMenu contextMenu = new ContextMenu(editMachineMenuItem, removeMachineMenuItem, editFileMenuItem,
						editExternalMenuItem, startAnimationMenu);

				machinesItem.setOnMouseClicked(event -> {
					if (event.getButton().equals(MouseButton.SECONDARY) && event.getClickCount() == 1) {
						updateAnimationMenu(startAnimationMenu, machine);
						contextMenu.show(machinesItem, event.getScreenX(), event.getScreenY());
					} else if (event.getButton().equals(MouseButton.PRIMARY) && event.getClickCount() == 2) {
						showDescription(machine);
					}
				});
			}
		});
		
		FontSize fontsize = injector.getInstance(FontSize.class);
		((FontAwesomeIconView) (addMachineButton.getGraphic())).glyphSizeProperty().bind(fontsize.multiply(2.0));
		((FontAwesomeIconView) (closeDescriptionButton.getGraphic())).glyphSizeProperty().bind(fontsize);
	}

	@FXML
	public void openHelp() {
		final Stage helpSystemStage = injector.getInstance(HelpSystemStage.class);
		helpSystemStage.show();
		helpSystemStage.toFront();
	}

	private void showDescription(Machine machine) {
		if (splitPane.getItems().size() < 2) {
			splitPane.getItems().add(0, descriptionView);
		}
		descriptionViewTitelLabel.textProperty().bind(new SimpleStringProperty(machine.getName()));
		descriptionText.textProperty().bind(new SimpleStringProperty(machine.getDescription()));
	}

	private void updateAnimationMenu(final Menu startAnimationMenu, Machine machine) {
		startAnimationMenu.getItems().clear();
		for (Runconfiguration runconfiguration : currentProject.getRunconfigurations(machine)) {
			final MenuItem item = new MenuItem(runconfiguration.toString());
			item.setOnAction(e -> currentProject.startAnimation(runconfiguration));
			startAnimationMenu.getItems().add(item);
		}
		if (startAnimationMenu.getItems().size() == 0) {
			startAnimationMenu.setDisable(true);
		}
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
			stageManager.makeAlert(Alert.AlertType.ERROR,
					"The machine \"" + machineFile + "\" already exists in the current project.").showAndWait();
			return;
		}
		injector.getInstance(AddMachinesDialog.class).showAndWait(relative);
	}

	@FXML
	private void closeDescriptionView() {
		splitPane.getItems().remove(descriptionView);
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
		final StateSpace stateSpace = ProBPreferences.getEmptyStateSpace(api, globalPreferences);
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
