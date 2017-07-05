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
import java.util.Set;

import org.hildan.fxgson.FxGson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.inject.Inject;

import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.project.preferences.DefaultPreference;
import de.prob2.ui.project.preferences.Preference;
import de.prob2.ui.project.runconfigurations.Runconfiguration;

public class ProjectManager {
	private static final Charset PROJECT_CHARSET = Charset.forName("UTF-8");
	private static final Logger LOGGER = LoggerFactory.getLogger(ProjectManager.class);

	private final Gson gson;
	private final CurrentProject currentProject;

	@Inject
	public ProjectManager(CurrentProject currentProject) {
		this.gson = FxGson.coreBuilder().disableHtmlEscaping().setPrettyPrinting().create();
		this.currentProject = currentProject;
	}

	public void saveCurrentProject() {
		Project project = currentProject.get();
		File loc = new File(project.getLocation() + File.separator + project.getName() + ".json");
		try (final Writer writer = new OutputStreamWriter(new FileOutputStream(loc), PROJECT_CHARSET)) {

			currentProject.update(new Project(project.getName(), project.getDescription(), project.getMachines(),
					project.getPreferences(), project.getRunconfigurations(), project.getLocation()));
			gson.toJson(project, writer);
		} catch (FileNotFoundException exc) {
			LOGGER.warn("Failed to create project data file", exc);
		} catch (IOException exc) {
			LOGGER.warn("Failed to save project", exc);
		}
		currentProject.setSaved(true);
	}

	public void openProject(File file) {
		Project project;
		try (final Reader reader = new InputStreamReader(new FileInputStream(file), PROJECT_CHARSET)) {
			project = gson.fromJson(reader, Project.class);
			project.setLocation(file.getParentFile());
			replaceMissingWithDefaults(project);
			setupRunconfigurations(project);
			initializeLTL(project);
		} catch (FileNotFoundException exc) {
			LOGGER.warn("Project file not found", exc);
			return;
		} catch (IOException exc) {
			LOGGER.warn("Failed to open project file", exc);
			return;
		}
		currentProject.set(project);
		currentProject.setSaved(true);
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
			if(p == null) {
				p = new DefaultPreference();
			}
			newRunconfigs.add(new Runconfiguration(m, p));
		}
		project.setRunconfigurations(newRunconfigs);
	}
	
	private void initializeLTL(Project project) {
		for(Machine machine: project.getMachines()) {
			machine.initializeLTLStatus();
			machine.initializeCBCStatus();
		}
	}

	private Map<String, Preference> getPreferencesAsMap(Project project) {
		Map<String, Preference> prefsMap = new HashMap<>();
		for(Preference p: project.getPreferences()) {
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
