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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hildan.fxgson.FxGson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.project.preferences.Preference;
import de.prob2.ui.project.runconfigurations.Runconfiguration;

public class ProjectManager {
	private static final Charset PROJECT_CHARSET = Charset.forName("UTF-8");
	private static final Logger LOGGER = LoggerFactory.getLogger(ProjectManager.class);

	private final Gson gson;
	private final CurrentProject currentProject;
	
	public ProjectManager(CurrentProject currentProject) {
		this.gson = FxGson.coreBuilder().disableHtmlEscaping().setPrettyPrinting().create();
		this.currentProject = currentProject;
	}

	public void save() {
		Project project = currentProject.get();
		File loc = new File(project.getLocation() + File.separator + project.getName() + ".json");
		try (final Writer writer = new OutputStreamWriter(new FileOutputStream(loc), PROJECT_CHARSET)) {

			currentProject.update(new Project(project.getName(), project.getDescription(), project.getMachines(), project.getPreferences(),
					project.getRunconfigurations(), project.getLocation()));
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
			project = replaceMissingWithDefaults(project);
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
	
	private Project replaceMissingWithDefaults(Project project) {
		String nameString = (project.getName() == null) ? "" : project.getName();
		String descriptionString = (project.getDescription() == null) ? "" : project.getDescription();
		List<Machine> machineList = new ArrayList<>();
		if (project.getMachines() != null) {
			machineList = project.getMachines();
			for (Machine machine : machineList) {
				machine.replaceMissingWithDefaults();
			}
		}
		List<Preference> preferenceList = (project.getPreferences() == null) ? new ArrayList<>()
				: project.getPreferences();
		Set<Runconfiguration> runconfigurationSet = (project.getRunconfigurations() == null) ? new HashSet<>()
				: project.getRunconfigurations();
		return new Project(nameString, descriptionString, machineList, preferenceList, runconfigurationSet,
				project.getLocation());
	}
}
