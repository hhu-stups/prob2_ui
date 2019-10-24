package de.prob2.ui.project;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.config.Config;
import de.prob2.ui.config.ConfigData;
import de.prob2.ui.config.ConfigListener;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.project.preferences.Preference;

import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.KeyCombination;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class ProjectManager {
	private static final Charset PROJECT_CHARSET = StandardCharsets.UTF_8;
	private static final Logger LOGGER = LoggerFactory.getLogger(ProjectManager.class);
	public static final String PROJECT_FILE_EXTENSION = "prob2project";
	public static final String PROJECT_FILE_PATTERN = "*." + PROJECT_FILE_EXTENSION;

	private final Gson gson;
	private final CurrentProject currentProject;
	private final StageManager stageManager;
	private final ResourceBundle bundle;
	
	private final ObservableList<Path> recentProjects;
	private final IntegerProperty maximumRecentProjects;

	@Inject
	public ProjectManager(Gson gson, CurrentProject currentProject, StageManager stageManager, ResourceBundle bundle, Config config) {
		this.gson = gson;
		this.currentProject = currentProject;
		this.stageManager = stageManager;
		this.bundle = bundle;
		
		this.recentProjects = FXCollections.observableArrayList();
		this.maximumRecentProjects = new SimpleIntegerProperty(this, "maximumRecentProjects");
		
		config.addListener(new ConfigListener() {
			@Override
			public void loadConfig(final ConfigData configData) {
				if (configData.maxRecentProjects > 0) {
					setMaximumRecentProjects(configData.maxRecentProjects);
				} else {
					setMaximumRecentProjects(10);
				}
				
				if (configData.recentProjects != null) {
					getRecentProjects().setAll(configData.recentProjects);
					truncateRecentProjects();
				}
			}
			
			@Override
			public void saveConfig(final ConfigData configData) {
				configData.maxRecentProjects = getMaximumRecentProjects();
				configData.recentProjects = new ArrayList<>(getRecentProjects());
			}
		});
	}

	public ObservableList<Path> getRecentProjects() {
		return this.recentProjects;
	}

	public IntegerProperty maximumRecentProjectsProperty() {
		return this.maximumRecentProjects;
	}

	public int getMaximumRecentProjects() {
		return this.maximumRecentProjectsProperty().get();
	}

	public void setMaximumRecentProjects(final int maximumRecentProjects) {
		this.maximumRecentProjectsProperty().set(maximumRecentProjects);
	}

	public void truncateRecentProjects() {
		if (getRecentProjects().size() > getMaximumRecentProjects()) {
			getRecentProjects().remove(getMaximumRecentProjects(), getRecentProjects().size());
		}
	}

	public List<MenuItem> getRecentProjectItems() {
		final List<MenuItem> newItems = new ArrayList<>();
		
		if (this.getRecentProjects().isEmpty()) {
			final MenuItem placeholderItem = new MenuItem(bundle.getString("menu.file.items.placeholder"));
			placeholderItem.setDisable(true);
			newItems.add(placeholderItem);
		} else {
			for (final Path path : this.getRecentProjects()) {
				final MenuItem item = new MenuItem(path.getFileName().toString());
				item.setOnAction(event -> this.openProject(path));
				newItems.add(item);
			}
			newItems.get(0).setAccelerator(KeyCombination.valueOf("Shift+Shortcut+'O'"));
		}
		
		newItems.add(new SeparatorMenuItem());
		
		final MenuItem clearRecentItem = new MenuItem(bundle.getString("menu.file.items.openRecentProject.items.clear"));
		clearRecentItem.setDisable(newItems.isEmpty());
		clearRecentItem.setOnAction(event -> this.getRecentProjects().clear());
		newItems.add(clearRecentItem);
		
		return newItems;
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
		File location = new File(project.getLocation() + File.separator + project.getName() + "." + PROJECT_FILE_EXTENSION);

		if (currentProject.isNewProject() && location.exists()) {
			ButtonType renameBT = new ButtonType((bundle.getString("common.buttons.rename")));
			ButtonType replaceBT = new ButtonType((bundle.getString("common.buttons.replace")));
			List<ButtonType> buttons = new ArrayList<>();
			buttons.add(replaceBT);
			buttons.add(renameBT);
			buttons.add(ButtonType.CANCEL);
			Optional<ButtonType> result = stageManager.makeAlert(AlertType.WARNING, buttons,
					"project.projectManager.alerts.fileAlreadyExistsWarning.header",
					"project.projectManager.alerts.fileAlreadyExistsWarning.content", location)
					.showAndWait();
			if (!result.isPresent() || result.get() == ButtonType.CANCEL) {
				return;
			} else if (result.get().equals(renameBT)) {
				FileChooser fileChooser = new FileChooser();
				fileChooser.setInitialDirectory(currentProject.getLocation().toFile());
				fileChooser.setInitialFileName(project.getName() + "." + PROJECT_FILE_EXTENSION);
				fileChooser.getExtensionFilters().add(new ExtensionFilter(String.format(bundle.getString("common.fileChooser.fileTypes.proB2Project"), PROJECT_FILE_PATTERN), PROJECT_FILE_PATTERN));
				location = fileChooser.showSaveDialog(stageManager.getCurrent());
				name = location.getName().substring(0, location.getName().lastIndexOf('.'));
			}
		}

		currentProject.set(new Project(name, project.getDescription(), project.getMachines(),
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
		try (final Reader reader = Files.newBufferedReader(path, PROJECT_CHARSET)) {
			final Project project = gson.fromJson(reader, Project.class);
			project.setLocation(path.getParent());
			return project;
		} catch (IOException | JsonSyntaxException exc) {
			LOGGER.warn("Failed to open project file", exc);
			List<ButtonType> buttons = new ArrayList<>();
			buttons.add(ButtonType.YES);
			buttons.add(ButtonType.NO);
			final String headerBundleKey;
			final String contentBundleKey;
			if (exc instanceof FileNotFoundException || exc instanceof NoSuchFileException) {
				headerBundleKey = "project.projectManager.alerts.fileNotFound.header";
				contentBundleKey = "project.projectManager.alerts.fileNotFound.content";
			} else {
				headerBundleKey = "project.projectManager.alerts.couldNotOpenFile.header";
				contentBundleKey = "project.projectManager.alerts.couldNotOpenFile.content";
			}
			Alert alert = stageManager.makeAlert(Alert.AlertType.ERROR, buttons, headerBundleKey, contentBundleKey, path);
			Optional<ButtonType> result = alert.showAndWait();
			if (result.isPresent() && result.get().equals(ButtonType.YES)) {
				Platform.runLater(() -> this.getRecentProjects().remove(path));
			}
			return null;
		}
	}

	public void openProject(Path path) {
		final Path absPath = path.toAbsolutePath();
		Project project = loadProject(absPath);
		if (project != null) {
			replaceMissingWithDefaults(project);
			project.getMachines().forEach(Machine::resetStatus);
			if(currentProject.switchTo(project, false)) {
				addToRecentProjects(absPath);
			}
		}
	}

	private void addToRecentProjects(Path path) {
		Platform.runLater(() -> {
			if (this.getRecentProjects().isEmpty() || !this.getRecentProjects().get(0).equals(path)) {
				this.getRecentProjects().remove(path);
				this.getRecentProjects().add(0, path);
				this.truncateRecentProjects();
			}
		});
	}

	public void openAutomaticProjectFromMachine(Path path) {
		final Path projectLocation = path.getParent();
		final Path relative = projectLocation.relativize(path);
		final String fileName = path.getFileName().toString();
		final String shortName = fileName.substring(0, fileName.lastIndexOf('.'));
		final String description = String.format(bundle.getString("menu.file.automaticProjectDescription"), path);
		final Machine machine = new Machine(shortName, "", relative);
		if (currentProject.switchTo(new Project(shortName, description, Collections.singletonList(machine), Collections.emptyList(), projectLocation), true)) {
			currentProject.startAnimation(machine, Preference.DEFAULT);
		}
	}

	public void openFile(final Path path) {
		if (com.google.common.io.Files.getFileExtension(path.getFileName().toString()).equals(PROJECT_FILE_EXTENSION)) {
			this.openProject(path);
		} else {
			this.openAutomaticProjectFromMachine(path);
		}
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
