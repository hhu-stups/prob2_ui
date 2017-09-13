package de.prob2.ui.menu;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.google.inject.Inject;
import com.google.inject.Injector;

import de.prob.exception.CliError;
import de.prob.exception.ProBError;
import de.prob.scripting.ModelTranslationError;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.NewProjectStage;
import de.prob2.ui.project.Project;
import de.prob2.ui.project.ProjectManager;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.project.preferences.DefaultPreference;
import de.prob2.ui.project.runconfigurations.Runconfiguration;
import de.prob2.ui.verifications.modelchecking.ModelcheckingController;

import javafx.application.Platform;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileMenu extends Menu {
	private static final Logger LOGGER = LoggerFactory.getLogger(FileMenu.class);

	@FXML
	private Menu recentProjectsMenu;
	@FXML
	private MenuItem recentProjectsPlaceholder;
	@FXML
	private MenuItem clearRecentProjects;
	@FXML
	private MenuItem saveProjectItem;
	@FXML
	private MenuItem reloadMachineItem;

	private final RecentProjects recentProjects;
	private final CurrentProject currentProject;
	private final CurrentTrace currentTrace;
	private final Injector injector;
	private final StageManager stageManager;

	@Inject
	private FileMenu(final StageManager stageManager, final RecentProjects recentProjects,
			final CurrentProject currentProject, final CurrentTrace currentTrace, final Injector injector) {
		this.recentProjects = recentProjects;
		this.currentProject = currentProject;
		this.currentTrace = currentTrace;
		this.injector = injector;
		this.stageManager = stageManager;
		stageManager.loadFXML(this, "fileMenu.fxml");
	}

	@FXML
	public void initialize() {
		final ListChangeListener<String> recentProjectsListener = change -> {
			final ObservableList<MenuItem> recentItems = this.recentProjectsMenu.getItems();
			final List<MenuItem> newItems = getRecentProjectItems(recentProjects);
			this.clearRecentProjects.setDisable(newItems.isEmpty());
			if (newItems.isEmpty()) {
				newItems.add(this.recentProjectsPlaceholder);
			}
			newItems.addAll(recentItems.subList(recentItems.size() - 2, recentItems.size()));
			this.recentProjectsMenu.getItems().setAll(newItems);
		};
		this.recentProjects.addListener(recentProjectsListener);
		recentProjectsListener.onChanged(null);

		this.saveProjectItem.disableProperty().bind(currentProject.existsProperty().not());

		this.reloadMachineItem.disableProperty().bind(currentTrace.existsProperty().not());
	}

	@FXML
	private void createNewProject() {
		final Stage newProjectStage = injector.getInstance(NewProjectStage.class);
		newProjectStage.showAndWait();
		newProjectStage.toFront();
	}

	@FXML
	private void handleOpen() {
		final File selected = stageManager.showOpenProjectOrMachineChooser(stageManager.getMainStage());
		if (selected == null) {
			return;
		}
		final String ext = StageManager.getExtension(selected.getName());
		if ("json".equals(ext)) {
			this.openProject(selected);
		} else {
			this.createProjectFromFile(selected);
		}
	}

	private void createProjectFromFile(File file) {
		final Path projectLocation = file.getParentFile().toPath();
		final Path absolute = file.toPath();
		final Path relative = projectLocation.relativize(absolute);
		final String shortName = file.getName().substring(0, file.getName().lastIndexOf('.'));
		final String description = "(this project was created automatically from file " + absolute + ')';
		final Machine machine = new Machine(shortName, "", relative);
		currentProject.set(new Project(shortName, description, machine, projectLocation.toFile()));

		final Runconfiguration defaultRunconfig = new Runconfiguration(machine, new DefaultPreference());
		currentProject.addRunconfiguration(defaultRunconfig);
		currentProject.startAnimation(defaultRunconfig);
	}

	@FXML
	private void handleOpenProject() {
		final FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Open Project");
		fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("ProB2 Projects", "*.json"));

		final File selectedProject = fileChooser.showOpenDialog(stageManager.getMainStage());
		if (selectedProject == null) {
			return;
		}

		this.openProject(selectedProject);
	}

	private void openProject(File file) {
		injector.getInstance(ProjectManager.class).openProject(file);

		Platform.runLater(() -> injector.getInstance(ModelcheckingController.class).resetView());
	}

	@FXML
	private void handleClearRecentProjects() {
		this.recentProjects.clear();
	}

	@FXML
	private void saveProject() {
		injector.getInstance(ProjectManager.class).saveCurrentProject();
	}

	@FXML
	private void handleReloadMachine() {
		try {
			this.currentTrace.reload(this.currentTrace.get());
		} catch (CliError | IOException | ModelTranslationError | ProBError e) {
			LOGGER.error("Model reload failed", e);
			stageManager.makeExceptionAlert(Alert.AlertType.ERROR, "Failed to reload model", e).showAndWait();
		}
	}

	@FXML
	private void handleClose() {
		final Stage stage = this.stageManager.getCurrent();
		if (stage != null) {
			stage.fireEvent(new WindowEvent(stage, WindowEvent.WINDOW_CLOSE_REQUEST));
		}
	}

	private List<MenuItem> getRecentProjectItems(SimpleListProperty<String> recentListProperty) {
		final List<MenuItem> newItems = new ArrayList<>();
		for (String s : recentListProperty) {
			File file = new File(s);
			final MenuItem item = new MenuItem(file.getName());
			item.setOnAction(event -> this.openProject(file));
			newItems.add(item);
		}
		return newItems;
	}
}
