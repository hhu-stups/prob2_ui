package de.prob2.ui.prob2fx;

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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.project.Machine;
import de.prob2.ui.project.Preference;
import de.prob2.ui.project.Project;
import de.prob2.ui.project.Runconfiguration;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyListProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

@Singleton
public final class CurrentProject extends SimpleObjectProperty<Project> {
	private static final Charset PROJECT_CHARSET = Charset.forName("UTF-8");
	private static final Logger LOGGER = LoggerFactory.getLogger(CurrentProject.class);

	private final Gson gson;

	private final BooleanProperty exists;
	private final StringProperty name;
	private final StringProperty description;
	private final ListProperty<Machine> machines;
	private final ListProperty<Preference> preferences;
	private final ReadOnlyListProperty<Runconfiguration> runconfigurations;
	private final ObjectProperty<File> location;

	private final ObjectProperty<Path> defaultLocation;
	private final StageManager stageManager;

	@Inject
	private CurrentProject(final StageManager stageManager) {
		this.stageManager = stageManager;

		this.gson = new GsonBuilder().setPrettyPrinting().create();
		this.defaultLocation = new SimpleObjectProperty<>(this, "defaultLocation",
				Paths.get(System.getProperty("user.home")));
		this.exists = new SimpleBooleanProperty(this, "exists", false);
		this.exists.bind(Bindings.isNotNull(this));

		this.name = new SimpleStringProperty(this, "name", "");
		this.description = new SimpleStringProperty(this, "description", "");
		this.machines = new SimpleListProperty<>(this, "machines", FXCollections.observableArrayList());
		this.preferences = new SimpleListProperty<>(this, "preferences", FXCollections.observableArrayList());
		this.runconfigurations = new SimpleListProperty<>(this, "runconfigurations",
				FXCollections.observableArrayList());
		this.location = new SimpleObjectProperty<>(this, "location", null);

		this.addListener((observable, from, to) -> {
			if (to == null) {
				this.name.set("");
				this.description.set("");
				this.machines.clear();
				this.preferences.clear();
				this.location.set(null);
			} else {
				this.name.set(to.getName());
				this.description.set(to.getDescription());
				this.machines.setAll(to.getMachines());
				this.preferences.setAll(to.getPreferences());
				this.runconfigurations.setAll(to.getRunconfigurations());
				this.location.set(to.getLocation());
			}
		});
	}

	public void addMachine(Machine machine) {
		List<Machine> machinesList = this.getMachines();
		machinesList.add(machine);
		this.update(new Project(this.getName(), this.getDescription(), machinesList, this.getPreferences(),
				this.getRunconfigurations(), this.getLocation()));
	}

	public void removeMachine(Machine machine) {
		List<Machine> machinesList = this.getMachines();
		machinesList.remove(machine);
		this.update(new Project(this.getName(), this.getDescription(), machinesList, this.getPreferences(),
				this.getRunconfigurations(), this.getLocation()));
	}
	
	public void updateMachine(Machine oldMachine, Machine newMachine) {
		this.removeMachine(oldMachine);
		this.addMachine(newMachine);
	}

	public void addPreference(Preference preference) {
		List<Preference> preferencesList = this.getPreferences();
		preferencesList.add(preference);
		this.update(new Project(this.getName(), this.getDescription(), this.getMachines(), preferencesList,
				this.getRunconfigurations(), this.getLocation()));
	}
	
	public void removePreference(Preference preference) {
		List<Preference> preferencesList = this.getPreferences();
		preferencesList.remove(preference);
		this.update(new Project(this.getName(), this.getDescription(), this.getMachines(), preferencesList,
				this.getRunconfigurations(), this.getLocation()));
	}

	public void addRunconfiguration(Runconfiguration runconfiguration) {
		List<Runconfiguration> runconfigs = this.getRunconfigurations();
		runconfigs.add(runconfiguration);
		this.update(new Project(this.getName(), this.getDescription(), this.getMachines(), this.getPreferences(),
				runconfigs, this.getLocation()));
	}
	
	public void removeRunconfiguration(Runconfiguration runconfiguration) {
		List<Runconfiguration> runconfigs = this.getRunconfigurations();
		runconfigs.remove(runconfiguration);
		this.update(new Project(this.getName(), this.getDescription(), this.getMachines(), this.getPreferences(),
				runconfigs, this.getLocation()));
	}

	@Override
	public void set(Project project) {
		if (confirmReplacingProject()) {
			super.set(project);
		}
	}

	private void update(Project project) {
		super.set(project);
	}

	public void remove() {
		super.set(null);
	}

	public ReadOnlyBooleanProperty existsProperty() {
		return this.exists;
	}

	public boolean exists() {
		return this.existsProperty().get();
	}

	public StringProperty nameProperty() {
		return this.name;
	}

	@Override
	public String getName() {
		return this.nameProperty().get();
	}

	public StringProperty descriptionProperty() {
		return this.description;
	}

	public String getDescription() {
		return this.descriptionProperty().get();
	}

	public ReadOnlyListProperty<Machine> machinesProperty() {
		return this.machines;
	}

	public List<Machine> getMachines() {
		return this.machinesProperty().get();
	}

	public ReadOnlyListProperty<Preference> preferencesProperty() {
		return this.preferences;
	}

	public List<Preference> getPreferences() {
		return this.preferencesProperty().get();
	}

	public ReadOnlyListProperty<Runconfiguration> runconfigurationsProperty() {
		return this.runconfigurations;
	}

	public List<Runconfiguration> getRunconfigurations() {
		return this.runconfigurationsProperty().get();
	}

	public ObjectProperty<File> locationProperty() {
		return this.location;
	}

	public File getLocation() {
		return this.locationProperty().get();
	}

	public ObjectProperty<Path> defaultLocationProperty() {
		return this.defaultLocation;
	}

	public Path getDefaultLocation() {
		return this.defaultLocationProperty().get();
	}

	public void setDefaultLocation(Path defaultProjectLocation) {
		this.defaultLocationProperty().set(defaultProjectLocation);
	}

	public void save() {
		File loc = new File(this.getLocation() + File.separator + this.getName() + ".json");
		try (final Writer writer = new OutputStreamWriter(new FileOutputStream(loc), PROJECT_CHARSET)) {
			gson.toJson(this.get(), writer);
		} catch (FileNotFoundException exc) {
			LOGGER.warn("Failed to create project data file", exc);
		} catch (IOException exc) {
			LOGGER.warn("Failed to save project", exc);
		}
	}

	public void open(File file) {
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
		this.set(project);
	}

	private Project replaceMissingWithDefaults(Project project) {
		String nameString = (project.getName() == null) ? "" : project.getName();
		String descriptionString = (project.getDescription() == null) ? "" : project.getDescription();
		List<Machine> machineList = (project.getMachines() == null) ? new ArrayList<>() : project.getMachines();
		List<Preference> preferenceList = (project.getPreferences() == null) ? new ArrayList<>()
				: project.getPreferences();
		Set<Runconfiguration> runconfigurationSet = (project.getRunconfigurations() == null) ? new HashSet<>()
				: project.getRunconfigurations();
		return new Project(nameString, descriptionString, machineList, preferenceList, runconfigurationSet,
				project.getLocation());
	}

	public Machine getMachine(String machine) {
		for (Machine m : getMachines()) {
			if (m.getName().equals(machine)) {
				return m;
			}
		}
		return null;
	}

	public Map<String, String> getPreferencAsMap(String preference) {
		for (Preference p : getPreferences()) {
			if (p.getName().equals(preference)) {
				return p.getPreferences();
			}
		}
		return null;
	}

	private boolean confirmReplacingProject() {
		if (exists()) {
			final Alert alert = stageManager.makeAlert(Alert.AlertType.CONFIRMATION);
			alert.setHeaderText("You've already opened a project.");
			alert.setContentText("Do you want to close the current project?");
			Optional<ButtonType> result = alert.showAndWait();
			return result.isPresent() && ButtonType.OK.equals(result.get());
		} else {
			return true;
		}
	}
}
