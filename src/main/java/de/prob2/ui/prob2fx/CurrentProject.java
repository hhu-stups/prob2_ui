package de.prob2.ui.prob2fx;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob2.ui.animation.symbolic.testcasegeneration.TestCaseGenerationView;
import de.prob2.ui.animation.tracereplay.TraceReplayView;
import de.prob2.ui.beditor.BEditorView;
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
					this.updateCurrentMachine(null, null);
				}
				for(Machine machine : to.getMachines()) {
					machine.changedProperty().addListener((o1, from1, to1) -> {
						if (to1) {
							this.setSaved(false);
						}
					});
				}
				for(Preference pref : to.getPreferences()) {
					pref.changedProperty().addListener((o1, from1, to1) -> {
						if (to1) {
							this.setSaved(false);
						}
					});
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

	private void updateCurrentMachine(final Machine m, final Preference p) {
		this.currentMachine.set(m);
		this.currentPreference.set(p);
		getCurrentMachine();
	}

	private void clearProperties() {
		this.name.set("");
		this.description.set("");
		this.machines.clear();
		this.preferences.clear();
		this.location.set(null);
		this.saved.set(true);
		this.updateCurrentMachine(null, null);
	}

	public void startAnimation(Machine m, Preference p) {
		injector.getInstance(BEditorView.class).getErrors().clear();
		MachineLoader machineLoader = injector.getInstance(MachineLoader.class);
		machineLoader.loadAsync(m, p.getPreferences());
		this.updateCurrentMachine(m, p);
		m.resetStatus();
		injector.getInstance(LTLView.class).bindMachine(m);
		injector.getInstance(SymbolicCheckingView.class).bindMachine(m);
		injector.getInstance(ModelcheckingView.class).bindMachine(m);
		injector.getInstance(TestCaseGenerationView.class).bindMachine(m);
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
		this.set(new Project(this.getName(), this.getDescription(), machinesList, this.getPreferences(), this.getLocation()));
	}

	public void removeMachine(Machine machine) {
		List<Machine> machinesList = this.getMachines();
		machinesList.remove(machine);
		if(machine.equals(currentMachine.get())) {
			this.saved.set(false);
			this.currentTrace.set(null);
			this.updateCurrentMachine(null, null);
		}
		this.set(new Project(this.getName(), this.getDescription(), machinesList, this.getPreferences(), this.getLocation()));
	}

	public void addPreference(Preference preference) {
		List<Preference> preferencesList = this.getPreferences();
		preferencesList.add(preference);
		this.set(new Project(this.getName(), this.getDescription(), this.getMachines(), preferencesList, this.getLocation()));
	}

	public void removePreference(Preference preference) {
		// If this is the last used preference for any machines, reset their last used preference to the default.
		this.getMachines().stream()
			.filter(machine -> preference.getName().equals(machine.getLastUsedPreferenceName()))
			.forEach(machine -> machine.setLastUsedPreferenceName(Preference.DEFAULT.getName()));
		List<Preference> preferencesList = this.getPreferences();
		preferencesList.remove(preference);
		this.set(new Project(this.getName(), this.getDescription(), this.getMachines(), preferencesList, this.getLocation()));
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

	public void changeName(String newName) {
		this.setNewProject(true);
		this.set(new Project(newName, this.getDescription(), this.getMachines(), this.getPreferences(), this.getLocation()));
	}

	public void changeDescription(String newDescription) {
		this.set(new Project(this.getName(), newDescription, this.getMachines(), this.getPreferences(), this.getLocation()));
	}

	public void switchTo(Project project, boolean newProject) {
		currentTrace.set(null);
		this.updateCurrentMachine(null, null);
		this.set(project);
		this.setSaved(true);
		this.setNewProject(newProject);
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

	public boolean confirmReplacingProject() {
		if (this.exists() && !this.isSaved()) {
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
