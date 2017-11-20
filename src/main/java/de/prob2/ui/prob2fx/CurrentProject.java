package de.prob2.ui.prob2fx;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

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
import de.prob2.ui.project.verifications.MachineTableView;
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
	private final ObjectProperty<Runconfiguration> currentRunconfiguration;
	private final ObjectProperty<Machine> currentMachine;

	private final ObjectProperty<File> location;
	private final BooleanProperty saved;

	private final ObjectProperty<Path> defaultLocation;
	private final StageManager stageManager;
	private final ResourceBundle bundle;
	private final Injector injector;
	private final AnimationSelector animations;
	private final CurrentTrace currentTrace;

	@Inject
	private CurrentProject(final StageManager stageManager, final ResourceBundle bundle, final Injector injector, final AnimationSelector animations,
							final CurrentTrace currentTrace) {
		this.stageManager = stageManager;
		this.bundle = bundle;
		this.injector = injector;
		this.animations = animations;
		this.currentTrace = currentTrace;

		this.defaultLocation = new SimpleObjectProperty<>(this, "defaultLocation",
				Paths.get(System.getProperty("user.home")));
		this.exists = new SimpleBooleanProperty(this, "exists", false);
		this.exists.bind(Bindings.isNotNull(this));
		this.name = new SimpleStringProperty(this, "name", "");
		this.description = new SimpleStringProperty(this, "description", "");
		this.machines = new SimpleListProperty<>(this, "machines", FXCollections.observableArrayList());
		this.preferences = new SimpleListProperty<>(this, "preferences", FXCollections.observableArrayList());
		this.currentRunconfiguration = new SimpleObjectProperty<>(this, "currentRunconfiguration", null);
		this.currentMachine = new SimpleObjectProperty<>(this, "currentMachine", null);
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
				this.location.set(to.getLocation());
			}
		});
		this.currentMachineProperty().addListener((observable, from, to) -> {
			if(to != null) {
				to.resetStatus();
			}
			injector.getInstance(MachineTableView.class).refresh();
		});
	}

	private void clearProperties() {
		this.name.set("");
		this.description.set("");
		this.machines.clear();
		this.preferences.clear();
		this.location.set(null);
		this.saved.set(true);
		this.currentRunconfiguration.set(null);
		this.currentMachine.set(null);
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
			this.currentMachine.set(runconfiguration.getMachine());
		} else {
			stageManager.makeAlert(Alert.AlertType.ERROR, String.format(bundle.getString("project.couldNotLoadMachine"), runconfiguration.getMachine(), runconfiguration.getPreference())).showAndWait();
		}
	}

	public void addMachine(Machine machine) {
		List<Machine> machinesList = this.getMachines();
		machinesList.add(machine);
		this.update(new Project(this.getName(), this.getDescription(), machinesList, this.getPreferences(), this.getLocation()));
	}

	public void removeMachine(Machine machine) {
		List<Machine> machinesList = this.getMachines();
		machinesList.remove(machine);
		if(machine.equals(currentMachine.get())) {
			this.saved.set(false);
			this.currentRunconfiguration.set(null);
			this.currentMachine.set(null);
			animations.removeTrace(currentTrace.get());
		}
		this.update(new Project(this.getName(), this.getDescription(), machinesList, this.getPreferences(), this.getLocation()));
	}

	public void addPreference(Preference preference) {
		List<Preference> preferencesList = this.getPreferences();
		preferencesList.add(preference);
		this.update(new Project(this.getName(), this.getDescription(), this.getMachines(), preferencesList, this.getLocation()));
	}

	public void removePreference(Preference preference) {
		List<Preference> preferencesList = this.getPreferences();
		preferencesList.remove(preference);
		this.update(new Project(this.getName(), this.getDescription(), this.getMachines(), preferencesList, this.getLocation()));
	}


	public ReadOnlyObjectProperty<Runconfiguration> currentRunconfigurationProperty() {
		return this.currentRunconfiguration;
	}

	public Runconfiguration getCurrentRunconfiguration() {
		return this.currentRunconfigurationProperty().get();
	}

	public ReadOnlyObjectProperty<Machine> currentMachineProperty() {
		return this.currentMachine;
	}

	public Machine getCurrentMachine() {
		return this.currentMachineProperty().get();
	}

	public void initializeMachines() {
		for (Machine machine : machines) {
			machine.resetStatus();
		}
	}

	public void changeName(String newName) {
		this.update(new Project(newName, this.getDescription(), this.getMachines(), this.getPreferences(), this.getLocation()));
	}

	public void changeDescription(String newDescription) {
		this.update(new Project(this.getName(), newDescription, this.getMachines(), this.getPreferences(), this.getLocation()));
	}

	@Override
	public void set(Project project) {
		if (!saved.get() && !confirmReplacingProject()) {
			return;
		}
		if (currentTrace.exists()) {
			animations.removeTrace(currentTrace.get());
		}
		update(project);
		initializeMachines();
		setSaved(true);
	}

	public void update(Project project) {
		super.set(project);
		for(Machine machine : project.getMachines()) {
			machine.changedProperty().addListener((observable, from, to) -> {
				if (to) {
					this.setSaved(false);
				}
			});
		}
		for(Preference pref : project.getPreferences()) {
			pref.changedProperty().addListener((observable, from, to) -> {
				if (to) {
					this.setSaved(false);
				}
			});
		}
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

	private boolean confirmReplacingProject() {
		if (exists()) {
			final Alert alert = stageManager.makeAlert(Alert.AlertType.CONFIRMATION);
			alert.setHeaderText(bundle.getString("project.confirmReplacingProject.header"));
			alert.setContentText(bundle.getString("project.confirmReplacingProject.content"));
			Optional<ButtonType> result = alert.showAndWait();
			return result.isPresent() && ButtonType.OK.equals(result.get());
		} else {
			return true;
		}
	}
}
