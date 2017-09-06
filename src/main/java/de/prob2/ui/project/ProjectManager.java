package de.prob2.ui.project;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.hildan.fxgson.FxGson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.menu.RecentProjects;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.project.preferences.DefaultPreference;
import de.prob2.ui.project.preferences.Preference;
import de.prob2.ui.project.runconfigurations.Runconfiguration;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;

@Singleton
public class ProjectManager {
	private static final Charset PROJECT_CHARSET = Charset.forName("UTF-8");
	private static final Logger LOGGER = LoggerFactory.getLogger(ProjectManager.class);

	private final Gson gson;
	private final CurrentProject currentProject;
	private final StageManager stageManager;
	private final RecentProjects recentProjects;

	@Inject
	public ProjectManager(CurrentProject currentProject, StageManager stageManager, RecentProjects recentProjects) {
		this.gson = FxGson.coreBuilder().disableHtmlEscaping().setPrettyPrinting().create();
		this.currentProject = currentProject;
		this.stageManager = stageManager;
		this.recentProjects = recentProjects;
	}
	
	private File saveProject(Project project) {
		File file = new File(project.getLocation() + File.separator + project.getName() + ".json");
		try (final Writer writer = new OutputStreamWriter(new FileOutputStream(file), PROJECT_CHARSET)) {
			gson.toJson(project, writer);
		} catch (FileNotFoundException exc) {
			LOGGER.warn("Failed to create project data file", exc);
			return null;
		} catch (IOException exc) {
			LOGGER.warn("Failed to save project", exc);
			return null;
		}
		return file;
	}

	public void saveCurrentProject() {
		Project project = currentProject.get();
		currentProject.update(new Project(project.getName(), project.getDescription(), project.getMachines(),
					project.getPreferences(), project.getRunconfigurations(), project.getLocation()));
		File savedFile = saveProject(project);
		if(savedFile != null) {
			addToRecentProjects(savedFile);
			currentProject.setSaved(true);
		}
	}
	
	private Project loadProject(File file) {
		Project project;
		try (final Reader reader = new InputStreamReader(new FileInputStream(file), PROJECT_CHARSET)) {
			project = gson.fromJson(reader, Project.class);
			project.setLocation(file.getParentFile());
		} catch (FileNotFoundException exc) {
			LOGGER.warn("Project file not found", exc);
			Alert alert = stageManager.makeAlert(AlertType.ERROR,
					"The project file " + file + " could not be found.\n"
							+ "The file was probably moved, renamed or deleted.\n\n"
							+ "Would you like to remove this project from the list of recent projects?",
					ButtonType.YES, ButtonType.NO);
			alert.setHeaderText("Project File not found.");
			Optional<ButtonType> result = alert.showAndWait();
			if (result.isPresent() && result.get().equals(ButtonType.YES)) {
				Platform.runLater(() -> recentProjects.remove(file.getAbsolutePath()));
			}
			return null;
		} catch (IOException exc) {
			LOGGER.warn("Failed to open project file", exc);
			return null;
		}
		return project;
	}

	public void openProject(File file) {
		Project project = loadProject(file);
		if(project != null) {
			replaceMissingWithDefaults(project);
			setupRunconfigurations(project);
			initializeLTL(project);
			currentProject.set(project);
			addToRecentProjects(file);
			currentProject.setSaved(true);
		} 
	}
	
	private void addToRecentProjects(File file) {
		Platform.runLater(() -> {
			if (recentProjects.isEmpty() || !recentProjects.get(0).equals(file.getAbsolutePath())) {
				this.recentProjects.remove(file.getAbsolutePath());
				this.recentProjects.add(0, file.getAbsolutePath());
			}
		});
	}

	private void replaceMissingWithDefaults(Project project) {
		project.setName((project.getName() == null) ? "" : project.getName());
		project.setDescription((project.getDescription() == null) ? "" : project.getDescription());
		List<Machine> machineList = new ArrayList<>();
		if (project.getMachines() != null) {
			machineList = project.getMachines();
			for (Machine machine : machineList) {
				machine.replaceMissingWithDefaults();
			}
		}
		project.setMachines(machineList);
		project.setPreferences((project.getPreferences() == null) ? new ArrayList<>() : project.getPreferences());
		project.setRunconfigurations(
				(project.getRunconfigurations() == null) ? new HashSet<>() : project.getRunconfigurations());
	}

	private void setupRunconfigurations(Project project) {
		Set<Runconfiguration> newRunconfigs = new HashSet<>();
		Map<String, Machine> machinesMap = getMachinesAsMap(project);
		Map<String, Preference> prefsMap = getPreferencesAsMap(project);
		for (Runconfiguration runconfig : project.getRunconfigurations()) {
			Machine m = machinesMap.get(runconfig.getMachineName());
			Preference p = prefsMap.get(runconfig.getPreferenceName());
			if (p == null) {
				p = new DefaultPreference();
			}
			newRunconfigs.add(new Runconfiguration(m, p));
		}
		project.setRunconfigurations(newRunconfigs);
	}

	private void initializeLTL(Project project) {
		for (Machine machine : project.getMachines()) {
			machine.initializeLTLStatus();
			machine.initializeCBCStatus();
		}
	}

	private Map<String, Preference> getPreferencesAsMap(Project project) {
		Map<String, Preference> prefsMap = new HashMap<>();
		for (Preference p : project.getPreferences()) {
			prefsMap.put(p.getName(), p);
		}
		return prefsMap;
	}

	private Map<String, Machine> getMachinesAsMap(Project project) {
		Map<String, Machine> machinesMap = new HashMap<>();
		for (Machine m : project.getMachines()) {
			machinesMap.put(m.getName(), m);
		}
		return machinesMap;
	}
}
