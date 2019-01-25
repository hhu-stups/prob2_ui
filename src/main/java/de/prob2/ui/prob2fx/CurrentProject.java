package de.prob2.ui.prob2fx;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob2.ui.animation.tracereplay.TraceReplayView;
import de.prob2.ui.config.Config;
import de.prob2.ui.config.ConfigData;
import de.prob2.ui.config.ConfigListener;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.project.MachineLoader;
import de.prob2.ui.project.Project;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.project.preferences.Preference;
import de.prob2.ui.project.verifications.MachineTableView;
import de.prob2.ui.statusbar.StatusBar;
import de.prob2.ui.verifications.ltl.LTLView;
import de.prob2.ui.verifications.modelchecking.ModelcheckingView;
import de.prob2.ui.verifications.symbolicchecking.SymbolicCheckingView;

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
	private final ObjectProperty<Machine> currentMachine;
	private final ObjectProperty<Preference> currentPreference;

	private final ObjectProperty<Path> location;
	private final BooleanProperty saved;
	private final BooleanProperty newProject;

	private final ObjectProperty<Path> defaultLocation;
	private final StageManager stageManager;
	private final Injector injector;
	private final CurrentTrace currentTrace;

	@Inject
	private CurrentProject(final StageManager stageManager, final Injector injector, final CurrentTrace currentTrace, final Config config) {
		this.stageManager = stageManager;
		this.injector = injector;
		this.currentTrace = currentTrace;

		this.defaultLocation = new SimpleObjectProperty<>(this, "defaultLocation",
				Paths.get(System.getProperty("user.home")));
		this.exists = new SimpleBooleanProperty(this, "exists", false);
		this.exists.bind(Bindings.isNotNull(this));
		this.name = new SimpleStringProperty(this, "name", "");
		this.description = new SimpleStringProperty(this, "description", "");
		this.machines = new SimpleListProperty<>(this, "machines", FXCollections.observableArrayList());
		this.preferences = new SimpleListProperty<>(this, "preferences", FXCollections.observableArrayList());
		this.currentMachine = new SimpleObjectProperty<>(this, "currentMachine", null);
		this.currentPreference = new SimpleObjectProperty<>(this, "currentPreference", null);
		this.location = new SimpleObjectProperty<>(this, "location", null);
		this.saved = new SimpleBooleanProperty(this, "saved", true);
		this.newProject = new SimpleBooleanProperty(this, "newProject", false);


		this.addListener((observable, from, to) -> {
			if (to == null) {
				clearProperties();
			} else {
				this.name.set(to.getName());
				this.description.set(to.getDescription());
				this.machines.setAll(to.getMachines());
				this.preferences.setAll(to.getPreferences());
				this.location.set(to.getLocation());
				this.saved.set(false);
				if (from != null && !to.getLocation().equals(from.getLocation())) {
					this.currentMachine.set(null);
				}
			}
		});
		this.currentMachineProperty().addListener((observable, from, to) -> {
			if(to != null) {
				to.resetStatus();
			}
			injector.getInstance(MachineTableView.class).refresh();
		});

		config.addListener(new ConfigListener() {
			@Override
			public void loadConfig(final ConfigData configData) {
				if (configData.defaultProjectLocation == null) {
					setDefaultLocation(Paths.get(System.getProperty("user.home")));
				} else {
					setDefaultLocation(Paths.get(configData.defaultProjectLocation));
				}
			}
			
			@Override
			public void saveConfig(final ConfigData configData) {
				configData.defaultProjectLocation = getDefaultLocation().toString();
			}
		});
	}

	private void clearProperties() {
		this.name.set("");
		this.description.set("");
		this.machines.clear();
		this.preferences.clear();
		this.location.set(null);
		this.saved.set(true);
		this.currentMachine.set(null);
		this.currentPreference.set(null);
	}

	public void startAnimation(Machine m, Preference p) {
		MachineLoader machineLoader = injector.getInstance(MachineLoader.class);
		machineLoader.loadAsync(m, p.getPreferences());
		this.currentMachine.set(m);
		this.currentPreference.set(p);
		m.resetStatus();
		injector.getInstance(LTLView.class).bindMachine(m);
		injector.getInstance(SymbolicCheckingView.class).bindMachine(m);
		injector.getInstance(ModelcheckingView.class).bindMachine(m);
		injector.getInstance(TraceReplayView.class).refresh();
		injector.getInstance(StatusBar.class).reset();
	}

	public void reloadCurrentMachine() {
		final Machine machine = this.getCurrentMachine();
		final Preference pref = this.getCurrentPreference();
		if (machine == null) {
			throw new IllegalStateException("Cannot reload without current machine");
		}
		if (pref == null) {
			throw new IllegalStateException("Cannot reload without current preference");
		}
		this.startAnimation(machine, pref);
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
			this.currentMachine.set(null);
			this.currentPreference.set(null);
			this.currentTrace.set(null);
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

	public ReadOnlyObjectProperty<Machine> currentMachineProperty() {
		return this.currentMachine;
	}

	public Machine getCurrentMachine() {
		return this.currentMachineProperty().get();
	}

	public ReadOnlyObjectProperty<Preference> currentPreferenceProperty() {
		return this.currentPreference;
	}

	public Preference getCurrentPreference() {
		return this.currentPreferenceProperty().get();
	}

	public void initializeMachines() {
		machines.forEach(Machine::resetStatus);
	}

	public void changeName(String newName) {
		this.setNewProject(true);
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
		currentTrace.set(null);
		update(project);
		initializeMachines();
	}
	
	public void set(Project project, boolean newProject) {
		if (!saved.get() && !confirmReplacingProject()) {
			return;
		}
		currentTrace.set(null);
		update(project);
		initializeMachines();
		this.setSaved(!newProject);
		this.setNewProject(newProject);
		this.currentMachine.set(null);
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

	public ObjectProperty<Path> locationProperty() {
		return this.location;
	}

	public Path getLocation() {
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
	
	public ReadOnlyBooleanProperty newProjectProperty() {
		return this.newProject;
	}

	public void setNewProject(boolean newProject) {
		this.newProject.set(newProject);
	}

	public boolean isNewProject() {
		return this.newProjectProperty().get();
	}

	private boolean confirmReplacingProject() {
		if (exists()) {
			final Alert alert = stageManager.makeAlert(Alert.AlertType.CONFIRMATION,
					"prob2fx.currentProject.alerts.confirmReplacingProject.header",
					"prob2fx.currentProject.alerts.confirmReplacingProject.content");
			Optional<ButtonType> result = alert.showAndWait();
			return result.isPresent() && ButtonType.OK.equals(result.get());
		} else {
			return true;
		}
	}
}
