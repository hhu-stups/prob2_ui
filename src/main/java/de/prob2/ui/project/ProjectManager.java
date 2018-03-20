package de.prob2.ui.project;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

import com.google.gson.Gson;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.menu.RecentProjects;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.project.preferences.Preference;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class ProjectManager {
	private static final Charset PROJECT_CHARSET = Charset.forName("UTF-8");
	private static final Logger LOGGER = LoggerFactory.getLogger(ProjectManager.class);

	private final Gson gson;
	private final CurrentProject currentProject;
	private final StageManager stageManager;
	private final ResourceBundle bundle;
	private final RecentProjects recentProjects;

	@Inject
	public ProjectManager(Gson gson, CurrentProject currentProject, StageManager stageManager, ResourceBundle bundle,
			RecentProjects recentProjects) {
		this.gson = gson;
		this.currentProject = currentProject;
		this.stageManager = stageManager;
		this.bundle = bundle;
		this.recentProjects = recentProjects;
	}

	private File saveProject(Project project, File location) {
		try (final Writer writer = new OutputStreamWriter(new FileOutputStream(location), PROJECT_CHARSET)) {
			gson.toJson(project, writer);
		} catch (FileNotFoundException exc) {
			LOGGER.warn("Failed to create project data file", exc);
			return null;
		} catch (IOException exc) {
			LOGGER.warn("Failed to save project", exc);
			return null;
		}
		return location;
	}

	public void saveCurrentProject() {
		Project project = currentProject.get();
		String name = project.getName();
		File location = new File(project.getLocation() + File.separator + project.getName() + ".json");

		if (currentProject.isNewProject() && location.exists()) {
			ButtonType renameBT = new ButtonType((bundle.getString("common.rename")));
			Optional<ButtonType> result = stageManager
					.makeAlert(AlertType.WARNING,
							String.format(bundle.getString("project.projectManager.fileAlreadyExistsWarning"),
									location),
							new ButtonType((bundle.getString("common.replace"))), renameBT, ButtonType.CANCEL)
					.showAndWait();
			if (!result.isPresent() || result.get() == ButtonType.CANCEL) {
				return;
			} else if (result.get().equals(renameBT)) {
				FileChooser fileChooser = new FileChooser();
				fileChooser.setInitialDirectory(currentProject.getLocation().toFile());
				fileChooser.setInitialFileName(project.getName() + ".json");
				fileChooser.getExtensionFilters().add(new ExtensionFilter("Project (*.json)", "*.json"));
				location = fileChooser.showSaveDialog(stageManager.getCurrent());
				name = location.getName().substring(0, location.getName().lastIndexOf('.'));
			}
		}

		currentProject.update(new Project(name, project.getDescription(), project.getMachines(),
				project.getPreferences(), project.getLocation()));

		File savedFile = saveProject(project, location);
		if (savedFile != null) {
			currentProject.setNewProject(false);
			addToRecentProjects(savedFile.toPath());
			currentProject.setSaved(true);
			for (Machine machine : currentProject.get().getMachines()) {
				machine.changedProperty().set(false);
			}
			for (Preference pref : currentProject.get().getPreferences()) {
				pref.changedProperty().set(false);
			}

		}

	}

	private Project loadProject(Path path) {
		Project project;
		try (final Reader reader = Files.newBufferedReader(path, PROJECT_CHARSET)) {
			project = gson.fromJson(reader, Project.class);
			project.setLocation(path.getParent());
		} catch (FileNotFoundException exc) {
			LOGGER.warn("Project file not found", exc);
			Alert alert = stageManager.makeAlert(Alert.AlertType.ERROR,
					String.format(bundle.getString("project.fileNotFound.content"), path), ButtonType.YES,
					ButtonType.NO);
			alert.setHeaderText(bundle.getString("project.fileNotFound.header"));
			Optional<ButtonType> result = alert.showAndWait();
			if (result.isPresent() && result.get().equals(ButtonType.YES)) {
				Platform.runLater(() -> recentProjects.remove(path.toAbsolutePath().toString()));
			}
			return null;
		} catch (IOException exc) {
			LOGGER.warn("Failed to open project file", exc);
			return null;
		}
		return project;
	}

	public void openProject(Path path) {
		Project project = loadProject(path);
		if (project != null) {
			replaceMissingWithDefaults(project);
			project.getMachines().forEach(Machine::resetStatus);
			currentProject.set(project, false);
			addToRecentProjects(path);
		}
	}

	private void addToRecentProjects(Path path) {
		Platform.runLater(() -> {
			final String absolutePath = path.toAbsolutePath().toString();
			if (recentProjects.isEmpty() || !recentProjects.get(0).equals(absolutePath)) {
				this.recentProjects.remove(absolutePath);
				this.recentProjects.add(0, absolutePath);
			}
		});
	}

	private void replaceMissingWithDefaults(Project project) {
		project.setName((project.getName() == null) ? "" : project.getName());
		project.setDescription((project.getDescription() == null) ? "" : project.getDescription());
		List<Machine> machineList = new ArrayList<>();
		if (project.getMachines() != null) {
			machineList = project.getMachines();
			machineList.forEach(Machine::replaceMissingWithDefaults);
		}
		project.setMachines(machineList);
		List<Preference> prefList = new ArrayList<>();
		if (project.getPreferences() != null) {
			prefList = project.getPreferences();
			prefList.forEach(Preference::replaceMissingWithDefaults);
		}
		project.setPreferences(prefList);
	}
}
