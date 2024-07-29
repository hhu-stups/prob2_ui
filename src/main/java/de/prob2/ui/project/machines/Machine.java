package de.prob2.ui.project.machines;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.google.common.base.MoreObjects;
import com.google.common.io.MoreFiles;

import de.prob.ltl.parser.pattern.PatternManager;
import de.prob.model.eventb.Context;
import de.prob.model.eventb.EventBMachine;
import de.prob.model.eventb.EventBModel;
import de.prob.model.eventb.ProofObligation;
import de.prob.model.representation.AbstractModel;
import de.prob.scripting.FactoryProvider;
import de.prob.scripting.ModelFactory;
import de.prob2.ui.animation.symbolic.SymbolicAnimationItem;
import de.prob2.ui.animation.symbolic.testcasegeneration.TestCaseGenerationItem;
import de.prob2.ui.animation.tracereplay.ReplayTrace;
import de.prob2.ui.dynamic.VisualizationFormulaTask;
import de.prob2.ui.internal.CachedEditorState;
import de.prob2.ui.project.preferences.Preference;
import de.prob2.ui.simulation.model.SimulationModel;
import de.prob2.ui.simulation.table.SimulationItem;
import de.prob2.ui.verifications.IValidationTask;
import de.prob2.ui.verifications.modelchecking.ModelCheckingItem;
import de.prob2.ui.verifications.po.ProofObligationItem;
import de.prob2.ui.verifications.symbolicchecking.SymbolicCheckingFormulaItem;
import de.prob2.ui.verifications.temporal.TemporalFormulaItem;
import de.prob2.ui.verifications.temporal.ltl.patterns.LTLPatternItem;
import de.prob2.ui.verifications.temporal.ltl.patterns.LTLPatternParser;
import de.prob2.ui.verifications.type.BuiltinValidationTaskTypes;

import javafx.beans.InvalidationListener;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyListProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@JsonPropertyOrder({
	"name",
	"description",
	"location",
	"lastUsedPreferenceName",
	"validationTasks",
	"ltlPatterns",
	"simulations",
	"visBVisualisation",
	"historyChartItems",
})
public final class Machine {
	private static final Logger LOGGER = LoggerFactory.getLogger(Machine.class);

	private final StringProperty name;
	private final StringProperty description;
	private final ObjectProperty<Path> location;
	private final StringProperty lastUsedPreferenceName;

	private final ReadOnlyListProperty<IValidationTask> validationTasks;
	private final ListProperty<LTLPatternItem> ltlPatterns;
	private final ListProperty<SimulationModel> simulations;
	private final ObjectProperty<Path> visBVisualisation;
	private final ListProperty<String> historyChartItems;

	private final ObservableList<ProofObligationItem> proofObligationTasks;

	@JsonIgnore
	private PatternManager patternManager;
	// For internal use by updateAllProofObligations:
	// stores the ProofObligation objects from the last model passed to updateAllProofObligationsFromModel.
	@JsonIgnore
	private List<? extends ProofObligation> lastModelProofObligations;
	@JsonIgnore
	private final ObservableList<ProofObligationItem> allProofObligations;
	@JsonIgnore
	private Map<Path, FileTime> sourceFileModifiedTimes;
	@JsonIgnore
	private final CachedEditorState cachedEditorState;
	@JsonIgnore
	private final BooleanProperty changed = new SimpleBooleanProperty(false);

	public Machine(final String name, final String description, final Path location) {
		this(
			name,
			description,
			location,
			null,
			Collections.emptyList(),
			Collections.emptyList(),
			Collections.emptyList(),
			null,
			Collections.emptyList()
		);
	}

	@JsonCreator
	public Machine(
		@JsonProperty("name") final String name,
		@JsonProperty("description") final String description,
		@JsonProperty("location") final Path location,
		@JsonProperty("lastUsedPreferenceName") final String lastUsedPreferenceName,
		@JsonProperty("validationTasks") final List<IValidationTask> validationTasks,
		@JsonProperty("ltlPatterns") final List<LTLPatternItem> ltlPatterns,
		@JsonProperty("simulations") final List<SimulationModel> simulations,
		@JsonProperty("visBVisualisation") final Path visBVisualisation,
		@JsonProperty("historyChartItems") final List<String> historyChartItems
	) {
		this.name = new SimpleStringProperty(this, "name", Objects.requireNonNull(name, "name"));
		this.description = new SimpleStringProperty(this, "description", Objects.requireNonNull(description, "description"));
		this.location = new SimpleObjectProperty<>(this, "location", Objects.requireNonNull(location, "location"));
		this.lastUsedPreferenceName = new SimpleStringProperty(this, "lastUsedPreferenceName", lastUsedPreferenceName != null && !lastUsedPreferenceName.isEmpty() ? lastUsedPreferenceName : Preference.DEFAULT.getName());

		this.validationTasks = new SimpleListProperty<>(this, "validationTasks", FXCollections.observableArrayList(validationTasks));
		this.ltlPatterns = new SimpleListProperty<>(this, "ltlPatterns", FXCollections.observableArrayList(ltlPatterns));
		this.simulations = new SimpleListProperty<>(this, "simulations", FXCollections.observableArrayList(simulations));
		this.visBVisualisation = new SimpleObjectProperty<>(this, "visBVisualisation", visBVisualisation);
		this.historyChartItems = new SimpleListProperty<>(this, "historyChartItems", FXCollections.observableArrayList(historyChartItems));

		// Keep this filtered list alive so that its listener (added in initListeners) keeps working.
		this.proofObligationTasks = this.getProofObligationTasks();

		this.patternManager = null;
		this.lastModelProofObligations = Collections.emptyList();
		this.allProofObligations = FXCollections.observableArrayList();
		this.sourceFileModifiedTimes = null;
		this.cachedEditorState = new CachedEditorState();

		this.initListeners();
	}

	private void initListeners() {
		final InvalidationListener changedListener = o -> this.setChanged(true);
		this.nameProperty().addListener(changedListener);
		this.descriptionProperty().addListener(changedListener);
		this.locationProperty().addListener(changedListener);
		this.lastUsedPreferenceNameProperty().addListener(changedListener);

		this.getValidationTasks().addListener(changedListener);
		this.getLTLPatterns().addListener(changedListener);
		this.getSimulations().addListener(changedListener);
		this.visBVisualizationProperty().addListener(changedListener);
		this.getHistoryChartItems().addListener(changedListener);

		this.proofObligationTasks.addListener((InvalidationListener)o -> this.updateAllProofObligations());
	}

	@JsonIgnore
	public Class<? extends ModelFactory<?>> getModelFactoryClass() {
		return FactoryProvider.factoryClassFromExtension(MoreFiles.getFileExtension(this.getLocation()));
	}

	public StringProperty lastUsedPreferenceNameProperty() {
		return this.lastUsedPreferenceName;
	}

	public String getLastUsedPreferenceName() {
		return this.lastUsedPreferenceNameProperty().get();
	}

	public void setLastUsedPreferenceName(final String lastUsedPreferenceName) {
		this.lastUsedPreferenceNameProperty().set(lastUsedPreferenceName);
	}

	public StringProperty nameProperty() {
		return this.name;
	}

	public String getName() {
		return this.nameProperty().get();
	}

	public void setName(final String name) {
		this.nameProperty().set(name);
	}

	public StringProperty descriptionProperty() {
		return this.description;
	}

	public String getDescription() {
		return this.descriptionProperty().get();
	}

	public void setDescription(final String description) {
		this.descriptionProperty().set(description);
	}

	public ObjectProperty<Path> locationProperty() {
		return this.location;
	}

	public Path getLocation() {
		return this.locationProperty().get();
	}

	public void setLocation(Path location) {
		this.locationProperty().set(location);
	}

	@JsonGetter("validationTasks")
	public ObservableList<IValidationTask> getValidationTasks() {
		return this.validationTasks;
	}

	@JsonIgnore
	private ObservableList<IValidationTask> getValidationTasksByPredicate(Predicate<IValidationTask> predicate) {
		return this.getValidationTasks().filtered(predicate);
	}

	@JsonIgnore
	public ObservableList<IValidationTask> getValidationTasksWithId() {
		return this.getValidationTasksByPredicate(vt -> vt.getId() != null);
	}

	@JsonIgnore
	public Map<String, IValidationTask> getValidationTasksById() {
		return this.getValidationTasks().stream()
			.filter(task -> task.getId() != null)
			.collect(Collectors.toMap(IValidationTask::getId, task -> task));
	}

	@JsonIgnore
	@SuppressWarnings("unchecked")
	public <T extends IValidationTask> ObservableList<T> getValidationTasksByClass(Class<T> clazz) {
		Objects.requireNonNull(clazz, "clazz");
		return (ObservableList<T>) this.getValidationTasksByPredicate(clazz::isInstance);
	}

	@JsonIgnore
	@SuppressWarnings("unchecked")
	public ObservableList<VisualizationFormulaTask> getVisualizationFormulaTasksByCommand(String command) {
		Objects.requireNonNull(command, "command");
		// casting shenanigans to make java's type inference happy
		return (ObservableList<VisualizationFormulaTask>) (ObservableList<?>) this.getValidationTasksByPredicate(vt -> BuiltinValidationTaskTypes.VISUALIZATION_FORMULA.equals(vt.getTaskType()) && command.equals(((VisualizationFormulaTask) vt).getCommandType()));
	}

	@JsonIgnore
	@SuppressWarnings("unchecked")
	public ObservableList<SimulationItem> getSimulationTasksByModel(SimulationModel model) {
		Objects.requireNonNull(model, "model");
		// casting shenanigans to make java's type inference happy
		return (ObservableList<SimulationItem>) (ObservableList<?>) this.getValidationTasksByPredicate(vt -> BuiltinValidationTaskTypes.SIMULATION.equals(vt.getTaskType()) && model.getPath().equals(((SimulationItem) vt).getSimulationPath()));
	}

	@JsonIgnore
	public Set<String> getValidationTaskIds() {
		return this.getValidationTasksWithId().stream()
			       .map(IValidationTask::getId)
			       .collect(Collectors.toSet());
	}

	public void addValidationTask(IValidationTask validationTask) {
		this.getValidationTasks().add(Objects.requireNonNull(validationTask, "validationTask"));
	}

	@SuppressWarnings("unchecked")
	public <T extends IValidationTask> T addValidationTaskIfNotExist(T validationTask) {
		Objects.requireNonNull(validationTask, "validationTask");
		Optional<IValidationTask> existingItem = this.getValidationTasks().stream().filter(validationTask::settingsEqual).findAny();
		if (existingItem.isPresent()) {
			IValidationTask vt = existingItem.get();
			vt.reset();
			return (T) vt;
		} else {
			this.addValidationTask(validationTask);
			return validationTask;
		}
	}

	public void removeValidationTask(IValidationTask validationTask) {
		this.getValidationTasks().remove(Objects.requireNonNull(validationTask, "validationTask"));
	}

	public void replaceValidationTask(IValidationTask oldValidationTask, IValidationTask newValidationTask) {
		Objects.requireNonNull(oldValidationTask, "oldValidationTask");
		Objects.requireNonNull(newValidationTask, "newValidationTask");
		int index = this.getValidationTasks().indexOf(oldValidationTask);
		if (index < 0) {
			throw new IllegalArgumentException("oldValidationTask not found");
		}

		this.getValidationTasks().set(index, newValidationTask);
	}

	@SuppressWarnings("unchecked")
	public <T extends IValidationTask> T replaceValidationTaskIfNotExist(T oldValidationTask, T newValidationTask) {
		Objects.requireNonNull(oldValidationTask, "oldValidationTask");
		Objects.requireNonNull(newValidationTask, "newValidationTask");
		Optional<IValidationTask> existingItem = this.getValidationTasks().stream().filter(newValidationTask::settingsEqual).findAny();
		if (existingItem.isPresent()) {
			IValidationTask vt = existingItem.get();
			vt.reset();
			return (T) vt;
		} else {
			this.replaceValidationTask(oldValidationTask, newValidationTask);
			return newValidationTask;
		}
	}

	@JsonIgnore
	public ObservableList<TemporalFormulaItem> getTemporalFormulas() {
		return this.getValidationTasksByClass(TemporalFormulaItem.class);
	}

	public ReadOnlyObjectProperty<MachineCheckingStatus> temporalStatusProperty() {
		return new MachineCheckingStatusProperty(this.getTemporalFormulas());
	}

	@JsonIgnore
	public ObservableList<SymbolicCheckingFormulaItem> getSymbolicCheckingFormulas() {
		return this.getValidationTasksByClass(SymbolicCheckingFormulaItem.class);
	}

	public ReadOnlyObjectProperty<MachineCheckingStatus> symbolicCheckingStatusProperty() {
		return new MachineCheckingStatusProperty(this.getSymbolicCheckingFormulas());
	}

	@JsonIgnore
	public ObservableList<ReplayTrace> getTraces() {
		return this.getValidationTasksByClass(ReplayTrace.class);
	}

	public ReadOnlyObjectProperty<MachineCheckingStatus> traceStatusProperty() {
		return new MachineCheckingStatusProperty(this.getTraces());
	}

	@JsonIgnore
	public ObservableList<ModelCheckingItem> getModelCheckingTasks() {
		return this.getValidationTasksByClass(ModelCheckingItem.class);
	}

	public ReadOnlyObjectProperty<MachineCheckingStatus> modelCheckingStatusProperty() {
		return new MachineCheckingStatusProperty(this.getModelCheckingTasks());
	}

	@JsonIgnore
	public ObservableList<ProofObligationItem> getProofObligationTasks() {
		return this.getValidationTasksByClass(ProofObligationItem.class);
	}

	@JsonGetter("ltlPatterns")
	public ReadOnlyListProperty<LTLPatternItem> getLTLPatterns() {
		return this.ltlPatterns;
	}

	@JsonIgnore
	public ObservableList<SymbolicAnimationItem> getSymbolicAnimationFormulas() {
		return this.getValidationTasksByClass(SymbolicAnimationItem.class);
	}

	@JsonIgnore
	public ObservableList<TestCaseGenerationItem> getTestCases() {
		return this.getValidationTasksByClass(TestCaseGenerationItem.class);
	}

	@JsonGetter("simulations")
	public ReadOnlyListProperty<SimulationModel> getSimulations() {
		return this.simulations;
	}

	public ObjectProperty<Path> visBVisualizationProperty() {
		return visBVisualisation;
	}

	@JsonGetter("visBVisualisation")
	public Path getVisBVisualisation() {
		return visBVisualisation.get();
	}

	public void setVisBVisualisation(Path visBVisualisation) {
		this.visBVisualizationProperty().set(visBVisualisation);
	}

	@JsonGetter("historyChartItems")
	public ReadOnlyListProperty<String> getHistoryChartItems() {
		return this.historyChartItems;
	}

	@JsonIgnore
	public PatternManager getPatternManager() {
		return patternManager;
	}

	public void reinitPatternManager() {
		this.getLTLPatterns().forEach(LTLPatternItem::reset);
		this.patternManager = new PatternManager();
		this.getLTLPatterns().forEach(item -> LTLPatternParser.addPattern(item, this));
	}

	public ObservableList<ProofObligationItem> getAllProofObligations() {
		return this.allProofObligations;
	}

	private static List<? extends ProofObligation> getProofObligationsFromModel(AbstractModel model) {
		// TODO: Does not yet work with .eventb files
		if (!(model instanceof EventBModel eventBModel)) {
			return Collections.emptyList();
		} else if (eventBModel.getMainComponent() instanceof EventBMachine machine) {
			return machine.getProofs();
		} else if (eventBModel.getMainComponent() instanceof Context context) {
			return context.getProofs();
		} else {
			return Collections.emptyList();
		}
	}

	private void updateAllProofObligations() {
		Map<String, ProofObligationItem> posWithIdByName = this.getProofObligationTasks().stream()
			.collect(Collectors.toMap(ProofObligationItem::getName, po -> po));

		// Update all existing PO tasks based on the PO information from the model.
		// This also removes all PO tasks from posWithIdByName that have a corresponding PO in the model.
		// After this loop has finished,
		// posWithIdByName will only contain the PO tasks for POs that no longer exist in the model.
		List<ProofObligationItem> updatedAllProofObligations = new ArrayList<>();
		for (ProofObligation po : this.lastModelProofObligations) {
			ProofObligationItem existingPoTask = posWithIdByName.remove(po.getName());
			if (existingPoTask != null) {
				assert existingPoTask.getId() != null;
				existingPoTask.updateFrom(po);
				updatedAllProofObligations.add(existingPoTask);
			} else {
				updatedAllProofObligations.add(new ProofObligationItem(po));
			}
		}

		// Finally, add all PO tasks that correspond to POs that no longer exist in the model.
		posWithIdByName.values().stream()
			.sorted(Comparator.comparing(ProofObligationItem::getName))
			.collect(Collectors.toCollection(() -> updatedAllProofObligations));

		// Store the updated POs in the machine.
		this.allProofObligations.setAll(updatedAllProofObligations);
	}

	public void updateAllProofObligationsFromModel(AbstractModel model) {
		this.lastModelProofObligations = getProofObligationsFromModel(model);
		this.updateAllProofObligations();
	}

	@JsonIgnore
	public CachedEditorState getCachedEditorState() {
		return cachedEditorState;
	}

	public BooleanProperty changedProperty() {
		return changed;
	}

	@JsonIgnore
	public boolean isChanged() {
		return this.changedProperty().get();
	}

	@JsonIgnore
	public void setChanged(final boolean changed) {
		this.changedProperty().set(changed);
	}

	public void resetAnimatorDependentState() {
		this.getValidationTasks().forEach(IValidationTask::resetAnimatorDependentState);
	}

	public void resetStatus() {
		for (var vt : this.getValidationTasks()) {
			vt.reset();
		}

		this.lastModelProofObligations.clear();
		this.allProofObligations.clear();
	}

	private static Map<Path, FileTime> readFileModifiedTimes(List<Path> files) throws IOException {
		Map<Path, FileTime> modifiedTimes = new HashMap<>();
		for (Path file : files) {
			modifiedTimes.put(file, Files.getLastModifiedTime(file));
		}
		return modifiedTimes;
	}

	public void updateModifiedTimesAndResetIfChanged(List<Path> newSourceFiles) {
		Map<Path, FileTime> newModifiedTimes;
		try {
			newModifiedTimes = readFileModifiedTimes(newSourceFiles);
		} catch (IOException exc) {
			LOGGER.warn("Failed to get last modified time for one of the source files - will assume that the model has changed", exc);
			newModifiedTimes = null;
		}

		// Consider the model changed if no modified times have been saved yet,
		// or the new modified times could not be read,
		// or the set of source files changed,
		// or if the saved and new modified times don't match for any of the files.
		if (this.sourceFileModifiedTimes == null || !this.sourceFileModifiedTimes.equals(newModifiedTimes)) {
			this.resetStatus();
		}

		this.sourceFileModifiedTimes = newModifiedTimes;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
			       .add("name", this.getName())
			       .add("location", this.getLocation())
			       .toString();
	}
}
