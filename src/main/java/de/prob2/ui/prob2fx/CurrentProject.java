package de.prob2.ui.prob2fx;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob.json.JsonMetadata;
import de.prob.statespace.StateSpace;
import de.prob2.ui.beditor.BEditorView;
import de.prob2.ui.config.Config;
import de.prob2.ui.config.ConfigData;
import de.prob2.ui.config.ConfigListener;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.internal.executor.CliTaskExecutor;
import de.prob2.ui.project.MachineLoader;
import de.prob2.ui.project.Project;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.project.preferences.Preference;
import de.prob2.ui.verifications.temporal.ltl.patterns.LTLPatternParser;
import de.prob2.ui.vomanager.Requirement;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
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
	private final ListProperty<Requirement> requirements;
	private final ListProperty<Preference> preferences;
	private final ObjectProperty<JsonMetadata> metadata;
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
		this.requirements = new SimpleListProperty<>(this, "requirements", FXCollections.observableArrayList());
		this.metadata = new SimpleObjectProperty<>(this, "metadata", null);
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
				this.requirements.setAll(to.getRequirements());
				this.preferences.setAll(to.getPreferences());
				this.metadata.set(to.getMetadata());
				this.location.set(to.getLocation());
				this.saved.set(false);
				if (from != null && !to.getLocation().equals(from.getLocation())) {
					this.updateCurrentMachine(null, null);
				}
				for (Machine machine : to.getMachines()) {
					machine.changedProperty().addListener((o1, from1, to1) -> {
						if (to1) {
							this.setSaved(false);
						}
					});
				}
				for (Preference pref : to.getPreferences()) {
					pref.changedProperty().addListener((o1, from1, to1) -> {
						if (to1) {
							this.setSaved(false);
						}
					});
				}
			}
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
	}

	private void clearProperties() {
		this.name.set("");
		this.description.set("");
		this.machines.clear();
		this.requirements.clear();
		this.preferences.clear();
		this.metadata.set(null);
		this.location.set(null);
		this.saved.set(true);
		this.updateCurrentMachine(null, null);
	}

	public CompletableFuture<?> startAnimation(Machine m, Preference p) {
		final StateSpace stateSpace = currentTrace.getStateSpace();
		if (stateSpace != null) {
			stateSpace.sendInterrupt();
		}
		injector.getInstance(CliTaskExecutor.class).interruptAll();
		injector.getInstance(BEditorView.class).getErrors().clear();
		MachineLoader machineLoader = injector.getInstance(MachineLoader.class);
		CompletableFuture<?> loadFuture = machineLoader.loadAsync(m, p.getPreferences());
		this.updateCurrentMachine(m, p);
		m.resetStatus();
		LTLPatternParser.parseMachine(m);
		return loadFuture;
	}

	public CompletableFuture<?> startAnimation(Machine m) {
		return this.startAnimation(m, this.get().getPreference(m.getLastUsedPreferenceName()));
	}

	public CompletableFuture<?> reloadCurrentMachine() {
		return this.reloadCurrentMachine(this.getCurrentPreference());
	}

	public CompletableFuture<?> reloadCurrentMachine(Preference preference) {
		return this.reloadMachine(this.getCurrentMachine(), preference);
	}

	public CompletableFuture<?> reloadMachine(Machine machine) {
		if (machine == null) {
			throw new IllegalStateException("Cannot reload without machine");
		} else if (!this.confirmMachineReplace()) {
			return CompletableFuture.completedFuture(null);
		}
		return this.startAnimation(machine);
	}

	public CompletableFuture<?> reloadMachine(Machine machine, Preference preference) {
		if (machine == null) {
			throw new IllegalStateException("Cannot reload without machine");
		} else if (preference == null) {
			throw new IllegalStateException("Cannot reload without preference");
		} else if (!this.confirmMachineReplace()) {
			return CompletableFuture.completedFuture(null);
		}
		return this.startAnimation(machine, preference);
	}

	public void addMachine(Machine machine) {
		List<Machine> machinesList = this.getMachines();
		machinesList.add(machine);
		this.set(new Project(this.getName(), this.getDescription(), machinesList, this.getRequirements(), this.getPreferences(), this.getMetadata(), this.getLocation()));
	}

	public void removeMachine(Machine machine) {
		List<Machine> machinesList = this.getMachines();
		machinesList.remove(machine);
		if (machine.equals(currentMachine.get())) {
			this.saved.set(false);
			this.currentTrace.set(null);
			this.updateCurrentMachine(null, null);
		}
		this.set(new Project(this.getName(), this.getDescription(), machinesList, this.getRequirements(), this.getPreferences(), this.getMetadata(), this.getLocation()));
	}

	public void changeMachineOrder(List<Machine> machines) {
		if (machines.size() != this.getMachines().size()) {
			throw new IllegalArgumentException("size mismatch, expected same number of machines");
		} else if (!new HashSet<>(this.getMachines()).containsAll(machines)) {
			throw new IllegalArgumentException("machine mismatch");
		}

		this.set(new Project(this.getName(), this.getDescription(), machines, this.getRequirements(), this.getPreferences(), this.getMetadata(), this.getLocation()));
	}

	public void addRequirement(Requirement requirement) {
		List<Requirement> requirementsList = this.getRequirements();
		requirementsList.add(requirement);
		this.set(new Project(this.getName(), this.getDescription(), this.getMachines(), requirementsList, this.getPreferences(), this.getMetadata(), this.getLocation()));
	}

	public void removeRequirement(Requirement requirement) {
		List<Requirement> requirementsList = this.getRequirements();
		requirementsList.remove(requirement);
		this.set(new Project(this.getName(), this.getDescription(), this.getMachines(), requirementsList, this.getPreferences(), this.getMetadata(), this.getLocation()));
	}

	public void replaceRequirement(final Requirement oldRequirement, final Requirement newRequirement) {
		List<Requirement> requirementsList = this.getRequirements();
		requirementsList.set(requirementsList.indexOf(oldRequirement), newRequirement);
		this.set(new Project(this.getName(), this.getDescription(), this.getMachines(), requirementsList, this.getPreferences(), this.getMetadata(), this.getLocation()));
	}

	public void addPreference(Preference preference) {
		List<Preference> preferencesList = this.getPreferences();
		preferencesList.add(preference);
		this.set(new Project(this.getName(), this.getDescription(), this.getMachines(), this.getRequirements(), preferencesList, this.getMetadata(), this.getLocation()));
	}

	public void removePreference(Preference preference) {
		// If this is the last used preference for any machines, reset their last used preference to the default.
		this.getMachines().stream()
			.filter(machine -> preference.getName().equals(machine.getLastUsedPreferenceName()))
			.forEach(machine -> machine.setLastUsedPreferenceName(Preference.DEFAULT.getName()));
		List<Preference> preferencesList = this.getPreferences();
		preferencesList.remove(preference);
		this.set(new Project(this.getName(), this.getDescription(), this.getMachines(), this.getRequirements(), preferencesList, this.getMetadata(), this.getLocation()));
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
		this.set(new Project(newName, this.getDescription(), this.getMachines(), this.getRequirements(), this.getPreferences(), this.getMetadata(), this.getLocation()));
	}

	public void changeDescription(String newDescription) {
		this.set(new Project(this.getName(), newDescription, this.getMachines(), this.getRequirements(), this.getPreferences(), this.getMetadata(), this.getLocation()));
	}

	public void switchTo(Project project, boolean newProject) {
		currentTrace.set(null);
		this.updateCurrentMachine(null, null);
		this.set(project);
		this.setSaved(true);
		this.setNewProject(newProject);
	}

	/**
	 * @deprecated Use {@link #isNotNull()} instead.
	 */
	@Deprecated
	public BooleanBinding existsProperty() {
		return this.isNotNull();
	}

	/**
	 * @deprecated Use a {@code != null} check instead.
	 */
	@Deprecated
	public boolean exists() {
		return this.get() != null;
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

	public ReadOnlyListProperty<Requirement> requirementsProperty() {
		return requirements;
	}

	public List<Requirement> getRequirements() {
		return requirements.get();
	}

	public ReadOnlyListProperty<Preference> preferencesProperty() {
		return this.preferences;
	}

	public List<Preference> getPreferences() {
		return this.preferencesProperty().get();
	}

	public ReadOnlyObjectProperty<JsonMetadata> metadataProperty() {
		return this.metadata;
	}

	public JsonMetadata getMetadata() {
		return this.metadataProperty().get();
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
		if (this.get() != null && !this.isSaved()) {
			final Alert alert = stageManager.makeAlert(Alert.AlertType.CONFIRMATION,
				"prob2fx.currentProject.alerts.confirmReplacingProject.header",
				"prob2fx.currentProject.alerts.confirmReplacingProject.content");
			Optional<ButtonType> result = alert.showAndWait();
			return result.isPresent() && ButtonType.OK.equals(result.get());
		} else {
			return true;
		}
	}

	public boolean confirmMachineReplace() {
		if (this.getCurrentMachine() == null || this.injector.getInstance(BEditorView.class).savedProperty().get()) {
			// we can replace the current machine when it is empty or saved
			return true;
		}

		final Alert alert = this.stageManager.makeAlert(Alert.AlertType.CONFIRMATION,
			"common.alerts.unsavedMachineChanges.header",
			"common.alerts.unsavedMachineChanges.content");
		Optional<ButtonType> result = alert.showAndWait();
		return result.isPresent() && ButtonType.OK.equals(result.get());
	}
}
