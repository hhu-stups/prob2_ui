package de.prob2.ui.project;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.MoreFiles;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob.json.JacksonManager;
import de.prob.json.JsonConversionException;
import de.prob2.ui.animation.tracereplay.ReplayTrace;
import de.prob2.ui.animation.tracereplay.TraceFileHandler;
import de.prob2.ui.config.Config;
import de.prob2.ui.config.ConfigData;
import de.prob2.ui.config.ConfigListener;
import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.project.preferences.Preference;

import de.prob2.ui.sharedviews.TraceViewHandler;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class ProjectManager {
	private static final Logger LOGGER = LoggerFactory.getLogger(ProjectManager.class);
	public static final String PROJECT_FILE_EXTENSION = "prob2project";

	private final JacksonManager<Project> jacksonManager;
	private final CurrentProject currentProject;
	private final StageManager stageManager;
	private final FileChooserManager fileChooserManager;
	private final ResourceBundle bundle;
	private final Injector injector;
	
	private final ObservableList<Path> recentProjects;
	private final IntegerProperty maximumRecentProjects;

	@Inject
	public ProjectManager(ObjectMapper objectMapper, JacksonManager<Project> jacksonManager, CurrentProject currentProject, StageManager stageManager,
						  ResourceBundle bundle, Config config, final FileChooserManager fileChooserManager, final Injector injector) {
		this.jacksonManager = jacksonManager;
		this.jacksonManager.initContext(new ProjectJsonContext(objectMapper));
		this.currentProject = currentProject;
		this.stageManager = stageManager;
		this.fileChooserManager = fileChooserManager;
		this.bundle = bundle;
		this.injector = injector;
		
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
				final MenuItem item = new MenuItem(MoreFiles.getNameWithoutExtension(path));
				// Stop JavaFX from looking for underscores in the menu item name
				// and interpreting them as mnemonics/key shortcuts
				// (can be seen when opening the menu using Alt on Windows).
				// Otherwise the first underscore (if any) in the project name disappears
				// and is incorrectly parsed as a mnemonic.
				item.setMnemonicParsing(false);
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

	public void saveCurrentProject() {
		Project project = currentProject.get();
		String name = project.getName();
		Path location = project.getLocation().resolve(project.getName() + "." + PROJECT_FILE_EXTENSION);

		if (currentProject.isNewProject() && Files.exists(location)) {
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
				fileChooser.getExtensionFilters().add(fileChooserManager.getExtensionFilter("common.fileChooser.fileTypes.proB2Project", PROJECT_FILE_EXTENSION));
				final Path selected = fileChooserManager.showSaveFileChooser(fileChooser, null, stageManager.getCurrent());
				if (selected == null) {
					return;
				}
				location = selected;
				name = MoreFiles.getNameWithoutExtension(location);
			}
		}

		injector.getInstance(TraceViewHandler.class).saveTraces(currentProject.get());

		// Change project name to new name selected by user (if necessary)
		// and update the metadata (replacing the metadata that was previously loaded from the file).
		final Project updatedProject = new Project(
			name,
			project.getDescription(),
			project.getMachines(),
			project.getPreferences(),
			Project.metadataBuilder().build(),
			project.getLocation()
		);
		currentProject.set(updatedProject);

		// To avoid corrupting the previously saved project if saving fails/is interrupted for some reason,
		// save the project under a temporary file name first,
		// and only once the project has been fully saved rename it to the real file name
		// (overwriting any existing project file with that name).
		final Path tempLocation = location.resolveSibling(location.getFileName() + ".tmp");
		try {
			this.jacksonManager.writeToFile(tempLocation, updatedProject);
			Files.move(tempLocation, location, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException | RuntimeException exc) {
			LOGGER.warn("Failed to save project", exc);
			stageManager.makeExceptionAlert(exc, "project.projectManager.alerts.failedToSaveProject.header", "project.projectManager.alerts.failedToSaveProject.content").show();
			try {
				Files.deleteIfExists(tempLocation);
			} catch (IOException e) {
				LOGGER.warn("Failed to delete temporary project file after project save error", e);
			}
			return;
		}

		currentProject.setNewProject(false);
		addToRecentProjects(location);
		currentProject.setSaved(true);
		currentProject.get().resetChanged();
	}

	private Project loadProject(Path path) {
		try {
			((ProjectJsonContext) this.jacksonManager.getContext()).setProjectLocation(path);
			final Project project = this.jacksonManager.readFromFile(path);
			project.setLocation(path.getParent());
			// Because Jackson fills in some parts of the project using setters (especially in Machine),
			// the project will be marked as changed immediately after loading,
			// which makes the project savedness tracking behave incorrectly.
			// To fix this, we forcibly mark the project as unchanged again after it is loaded.
			project.resetChanged();
			return project;
		} catch (IOException | JsonConversionException exc) {
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
			boolean replacingProject = currentProject.confirmReplacingProject();
			if(replacingProject) {
				currentProject.switchTo(project, false);
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
		final String shortName = MoreFiles.getNameWithoutExtension(path);
		final String description = String.format(bundle.getString("menu.file.automaticProjectDescription"), path);
		final Machine machine = new Machine(shortName, "", relative);
		boolean replacingProject = currentProject.confirmReplacingProject();
		if(replacingProject) {
			currentProject.switchTo(new Project(shortName, description, Collections.singletonList(machine), Collections.emptyList(), Project.metadataBuilder().build(), projectLocation), true);
			currentProject.startAnimation(machine, Preference.DEFAULT);
		}
	}

	public void openFile(final Path path) {
		if (MoreFiles.getFileExtension(path).equals(PROJECT_FILE_EXTENSION)) {
			this.openProject(path);
		} else {
			this.openAutomaticProjectFromMachine(path);
		}
	}
	
}
