package de.prob2.ui.project;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.animation.symbolic.testcasegeneration.TestCaseGenerationItem;
import de.prob2.ui.animation.symbolic.testcasegeneration.TestCaseGenerationType;
import de.prob2.ui.config.Config;
import de.prob2.ui.config.ConfigData;
import de.prob2.ui.config.ConfigListener;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.json.JsonManager;
import de.prob2.ui.json.JsonMetadata;
import de.prob2.ui.json.ObjectWithMetadata;
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
	private static final Logger LOGGER = LoggerFactory.getLogger(ProjectManager.class);
	public static final String PROJECT_FILE_EXTENSION = "prob2project";
	public static final String PROJECT_FILE_PATTERN = "*." + PROJECT_FILE_EXTENSION;

	private final JsonManager<Project> jsonManager;
	private final CurrentProject currentProject;
	private final StageManager stageManager;
	private final ResourceBundle bundle;
	
	private final ObservableList<Path> recentProjects;
	private final IntegerProperty maximumRecentProjects;

	@Inject
	public ProjectManager(JsonManager<Project> jsonManager, CurrentProject currentProject, StageManager stageManager, ResourceBundle bundle, Config config) {
		this.jsonManager = jsonManager;
		this.jsonManager.initContext(new JsonManager.Context<Project>(Project.class, "Project", 1) {
			private void updateV0CheckableItem(final JsonObject checkableItem) {
				if (!checkableItem.has("selected")) {
					checkableItem.addProperty("selected", true);
				}
			}
			
			private void updateV0TestCaseItem(final JsonObject testCaseItem) {
				if (!testCaseItem.has("additionalInformation")) {
					testCaseItem.add("additionalInformation", new JsonObject());
				}
				final JsonObject additionalInformation = testCaseItem.getAsJsonObject("additionalInformation");
				final String type = testCaseItem.get("type").getAsString();
				final String code = testCaseItem.get("code").getAsString();
				// Old UI versions did not have the additionalInformation map
				// and instead stored additional information in the code string.
				// The additionalInformation map may also have been deleted
				// when test case generation was separated from symbolic animation
				// (see moveV0TestCaseSymbolicAnimationItems for details).
				// In either of these cases, the additionalInformation map will be missing/empty,
				// and its contents need to be extracted from the code string.
				final String[] splitOnSlash = code.replace(" ", "").split("/");
				final String[] splitFirstOnColon = splitOnSlash[0].split(":");
				if (TestCaseGenerationType.MCDC.name().equals(type)) {
					if (!additionalInformation.has(TestCaseGenerationItem.LEVEL)) {
						if (!"MCDC".equals(splitFirstOnColon[0])) {
							throw new JsonParseException("First part of MCDC item code string does not contain level: " + splitOnSlash[0]);
						}
						additionalInformation.addProperty(TestCaseGenerationItem.LEVEL, Integer.parseInt(splitFirstOnColon[1]));
					}
				} else if (TestCaseGenerationType.COVERED_OPERATIONS.name().equals(type)) {
					if (!additionalInformation.has(TestCaseGenerationItem.OPERATIONS)) {
						if (!"OPERATION".equals(splitFirstOnColon[0])) {
							throw new JsonParseException("First part of covered operations item code string does not contain operations: " + splitOnSlash[0]);
						}
						final String[] operationNames = splitFirstOnColon[1].split(",");
						final JsonArray operationNamesJsonArray = new JsonArray(operationNames.length);
						for (final String operationName : operationNames) {
							operationNamesJsonArray.add(operationName);
						}
						additionalInformation.add(TestCaseGenerationItem.OPERATIONS, operationNamesJsonArray);
					}
				}
				if (!testCaseItem.has("maxDepth")) {
					// Test case items moved from symbolic animation items may have the maxDepth field missing.
					// In this case, the depth value needs to be extracted from the code string.
					final String[] depthSplitOnColon = splitOnSlash[1].split(":");
					if (!"DEPTH".equals(depthSplitOnColon[0])) {
						throw new JsonParseException("Second part of test case item code string does not contain depth: " + splitOnSlash[1]);
					}
					testCaseItem.addProperty("maxDepth", Integer.parseInt(depthSplitOnColon[1]));
				}
			}
			
			private void moveV0TestCaseSymbolicAnimationItems(final JsonArray symbolicAnimationFormulas, final JsonArray testCases) {
				// Test case generation was previously part of symbolic animation,
				// but has now been moved into its own checking category.
				// Projects from older versions may still contain symbolic animation items for test case generation,
				// which need to be converted to proper test case generation items.
				for (final Iterator<JsonElement> it = symbolicAnimationFormulas.iterator(); it.hasNext();) {
					final JsonObject symbolicAnimationFormula = it.next().getAsJsonObject();
					final TestCaseGenerationType testCaseGenerationType;
					final JsonElement typeElement = symbolicAnimationFormula.get("type");
					if (typeElement.isJsonNull()) {
						// If a project contains symbolic animation items for test case generation,
						// and it is loaded and re-saved by a newer version that has separated test case generation (but no file format versioning/conversion),
						// the symbolic animation items will have their type silently set to null,
						// because the corresponding enum items have been removed.
						// In this case, the type needs to be restored from the code string.
						final String code = symbolicAnimationFormula.get("code").getAsString();
						if (code.startsWith("MCDC")) {
							testCaseGenerationType = TestCaseGenerationType.MCDC;
						} else if (code.startsWith("OPERATION")) {
							testCaseGenerationType = TestCaseGenerationType.COVERED_OPERATIONS;
						} else {
							testCaseGenerationType = null;
						}
					} else if ("MCDC".equals(typeElement.getAsString())) {
						testCaseGenerationType = TestCaseGenerationType.MCDC;
					} else if ("COVERED_OPERATIONS".equals(typeElement.getAsString())) {
						testCaseGenerationType = TestCaseGenerationType.COVERED_OPERATIONS;
					} else {
						testCaseGenerationType = null;
					}
					if (testCaseGenerationType != null) {
						// If this item is for test case generation, move it into the list of test case items.
						it.remove();
						testCases.add(symbolicAnimationFormula);
						// Update/fix the type, as determined above.
						symbolicAnimationFormula.addProperty("type", testCaseGenerationType.name());
						// In symbolic animation items, the maxDepth value was stored in the additionalInformation map.
						// In test case items, it is stored as a regular field.
						// If the additionalInformation map is missing,
						// the maxDepth value needs to be extracted from the code field instead.
						// That case is handled in updateV0TestCaseItem.
						final JsonElement additionalInformationElement = symbolicAnimationFormula.get("additionalInformation");
						if (additionalInformationElement != null && additionalInformationElement.isJsonObject()) {
							symbolicAnimationFormula.add("maxDepth", additionalInformationElement.getAsJsonObject().remove("maxDepth"));
						}
					}
				}
			}
			
			private void updateV0Machine(final JsonObject machine) {
				if (!machine.has("lastUsedPreferenceName")) {
					final String lastUsedPreferenceName;
					if (machine.has("lastUsed")) {
						lastUsedPreferenceName = machine.getAsJsonObject("lastUsed").get("name").getAsString();
					} else {
						lastUsedPreferenceName = Preference.DEFAULT.getName();
					}
					machine.addProperty("lastUsedPreferenceName", lastUsedPreferenceName);
				}
				for (final String checkableItemFieldName : new String[] {"ltlFormulas", "ltlPatterns", "symbolicCheckingFormulas", "symbolicAnimationFormulas", "testCases"}) {
					if (!machine.has(checkableItemFieldName)) {
						machine.add(checkableItemFieldName, new JsonArray());
					}
					machine.getAsJsonArray(checkableItemFieldName).forEach(checkableItemElement ->
						this.updateV0CheckableItem(checkableItemElement.getAsJsonObject())
					);
				}
				final JsonArray testCases = machine.getAsJsonArray("testCases");
				final JsonArray symbolicAnimationFormulas = machine.getAsJsonArray("symbolicAnimationFormulas");
				moveV0TestCaseSymbolicAnimationItems(symbolicAnimationFormulas, testCases);
				testCases.forEach(testCaseItemElement ->
					this.updateV0TestCaseItem(testCaseItemElement.getAsJsonObject())
				);
				if (!machine.has("traces")) {
					machine.add("traces", new JsonArray());
				}
				if (!machine.has("modelcheckingItems")) {
					machine.add("modelcheckingItems", new JsonArray());
				}
			}
			
			private void updateV0Project(final JsonObject project) {
				if (!project.has("name")) {
					project.addProperty("name", "");
				}
				if (!project.has("description")) {
					project.addProperty("description", "");
				}
				if (!project.has("machines")) {
					project.add("machines", new JsonArray());
				}
				project.getAsJsonArray("machines").forEach(machineElement ->
					this.updateV0Machine(machineElement.getAsJsonObject())
				);
				if (!project.has("preferences")) {
					project.add("preferences", new JsonArray());
				}
			}
			
			@Override
			public ObjectWithMetadata<JsonObject> convertOldData(final JsonObject oldObject, final JsonMetadata oldMetadata) {
				if (oldMetadata.getFormatVersion() <= 0) {
					updateV0Project(oldObject);
				}
				return new ObjectWithMetadata<>(oldObject, oldMetadata);
			}
		});
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
		try {
			this.jsonManager.writeToFile(location.toPath(), project);
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
		try {
			final Project project = this.jsonManager.readFromFile(path).getObject();
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
		final String fileName = path.getFileName().toString();
		final String shortName = fileName.substring(0, fileName.lastIndexOf('.'));
		final String description = String.format(bundle.getString("menu.file.automaticProjectDescription"), path);
		final Machine machine = new Machine(shortName, "", relative);
		boolean replacingProject = currentProject.confirmReplacingProject();
		if(replacingProject) {
			currentProject.switchTo(new Project(shortName, description, Collections.singletonList(machine), Collections.emptyList(), projectLocation), true);
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
	
}
