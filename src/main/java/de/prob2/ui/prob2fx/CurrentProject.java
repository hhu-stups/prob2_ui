package de.prob2.ui.prob2fx;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob.statespace.AnimationSelector;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.project.MachineLoader;
import de.prob2.ui.project.Project;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.project.preferences.DefaultPreference;
import de.prob2.ui.project.preferences.Preference;
import de.prob2.ui.project.runconfigurations.Runconfiguration;
import de.prob2.ui.verifications.modelchecking.ModelcheckingController;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyListProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
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
	private final BooleanProperty exists;
	private final StringProperty name;
	private final StringProperty description;
	private final ListProperty<Machine> machines;
	private final ListProperty<Preference> preferences;
	private final ReadOnlyListProperty<Runconfiguration> runconfigurations;
	private final ObjectProperty<Runconfiguration> currentRunconfiguration;

	private final ObjectProperty<File> location;
	private final BooleanProperty saved;

	private final ObjectProperty<Path> defaultLocation;
	private final StageManager stageManager;
	private final Injector injector;
	private final AnimationSelector animations;
	private final CurrentTrace currentTrace;
	private final ModelcheckingController modelCheckController;

	@Inject
	private CurrentProject(final StageManager stageManager, final Injector injector, final AnimationSelector animations,
			final CurrentTrace currentTrace, final ModelcheckingController modelCheckController) {
		this.stageManager = stageManager;
		this.injector = injector;
		this.animations = animations;
		this.currentTrace = currentTrace;
		this.modelCheckController = modelCheckController;

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
		this.currentRunconfiguration = new SimpleObjectProperty<>(this, "currentRunconfiguration", null);
		this.location = new SimpleObjectProperty<>(this, "location", null);
		this.saved = new SimpleBooleanProperty(this, "saved", true);

		this.addListener((observable, from, to) -> {
			if (to == null) {
				clearProperties();
			} else {
				this.name.set(to.getName());
				this.description.set(to.getDescription());
				this.machines.setAll(to.getMachines());
				this.preferences.setAll(to.getPreferences());
				this.runconfigurations.setAll(to.getRunconfigurations());
				this.location.set(to.getLocation());
				if (!to.equals(from)) {
					this.saved.set(false);
				}
			}
		});
	}

	private void clearProperties() {
		this.name.set("");
		this.description.set("");
		this.machines.clear();
		this.preferences.clear();
		this.location.set(null);
		this.injector.getInstance(ModelcheckingController.class).resetView();
		this.saved.set(true);
	}

	public void startAnimation(Runconfiguration runconfiguration) {
		Machine m = runconfiguration.getMachine();
		Map<String, String> pref = new HashMap<>();
		if (!(runconfiguration.getPreference() instanceof DefaultPreference)) {
			pref = runconfiguration.getPreference().getPreferences();
		}
		if (m != null && pref != null) {
			MachineLoader machineLoader = injector.getInstance(MachineLoader.class);
			machineLoader.loadAsync(m, pref);
			this.currentRunconfiguration.set(runconfiguration);
		} else {
			stageManager.makeAlert(Alert.AlertType.ERROR, "Could not load machine \"" + runconfiguration.getMachine()
					+ "\" with preferences: \"" + runconfiguration.getPreference() + "\"").showAndWait();
		}
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
		List<Runconfiguration> runconfigsList = new ArrayList<>();
		runconfigsList.addAll(this.getRunconfigurations());
		this.getRunconfigurations(machine).stream().forEach(runconfigsList::remove);
		this.update(new Project(this.getName(), this.getDescription(), machinesList, this.getPreferences(),
				runconfigsList, this.getLocation()));
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
		List<Runconfiguration> runconfigsList = new ArrayList<>();
		runconfigsList.addAll(this.getRunconfigurations());
		this.getRunconfigurations().stream().filter(r -> r.getPreference().equals(preference.getName()))
				.forEach(runconfigsList::remove);
		this.update(new Project(this.getName(), this.getDescription(), this.getMachines(), preferencesList,
				runconfigsList, this.getLocation()));
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

	public List<Runconfiguration> getRunconfigurations(Machine machine) {
		return getRunconfigurations().stream().filter(runconfig -> machine.equals(runconfig.getMachine()))
				.collect(Collectors.toList());
	}
	
	public ReadOnlyObjectProperty<Runconfiguration> currentRunconfigurationProperty() {
		return this.currentRunconfiguration;
	}
	
	public Runconfiguration getCurrentRunconfiguration() {
		return this.currentRunconfigurationProperty().get();
	}

	public void initializeMachines() {
		for (Machine machine : machines) {
			machine.initializeStatus();
		}
	}

	public void changeName(String newName) {
		this.update(new Project(newName, this.getDescription(), this.getMachines(), this.getPreferences(),
				this.getRunconfigurations(), this.getLocation()));
	}

	public void changeDescription(String newDescription) {
		this.update(new Project(this.getName(), newDescription, this.getMachines(), this.getPreferences(),
				this.getRunconfigurations(), this.getLocation()));
	}

	@Override
	public void set(Project project) {
		if (!saved.get() && !confirmReplacingProject()) {
			return;
		}
		currentRunconfiguration.set(null);
		if (currentTrace.exists()) {
			animations.removeTrace(currentTrace.get());
			modelCheckController.resetView();
		}
		super.set(project);
		initializeMachines();
	}

	public void update(Project project) {
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

	public ReadOnlyBooleanProperty savedProperty() {
		return this.saved;
	}

	public void setSaved(boolean saved) {
		this.saved.set(saved);
	}

	public boolean isSaved() {
		return this.savedProperty().get();
	}

	public Machine getMachine(String machine) {
		for (Machine m : getMachines()) {
			if (m.getName().equals(machine)) {
				return m;
			}
		}
		return null;
	}

	public Map<String, String> getPreferenceAsMap(String preference) {
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
			alert.setContentText("Do you want to close the current project?\n(Unsaved changes will be lost)");
			Optional<ButtonType> result = alert.showAndWait();
			return result.isPresent() && ButtonType.OK.equals(result.get());
		} else {
			return true;
		}
	}
}
