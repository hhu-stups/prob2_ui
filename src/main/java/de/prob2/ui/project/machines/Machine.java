package de.prob2.ui.project.machines;

import java.nio.file.Path;
import java.util.List;
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
import com.fasterxml.jackson.annotation.JsonSetter;
import com.google.common.base.MoreObjects;
import com.google.common.io.MoreFiles;

import de.prob.ltl.parser.pattern.PatternManager;
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
import de.prob2.ui.verifications.modelchecking.ModelCheckingItem;
import de.prob2.ui.verifications.po.ProofObligationItem;
import de.prob2.ui.verifications.symbolicchecking.SymbolicCheckingFormulaItem;
import de.prob2.ui.verifications.temporal.TemporalFormulaItem;
import de.prob2.ui.verifications.temporal.ltl.patterns.LTLPatternItem;
import de.prob2.ui.verifications.type.BuiltinValidationTaskTypes;
import de.prob2.ui.verifications.type.ValidationTaskType;
import de.prob2.ui.vomanager.IValidationTask;

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

	private final StringProperty name;
	private final StringProperty description;
	private final Path location;
	private final StringProperty lastUsedPreferenceName;

	private final ReadOnlyListProperty<IValidationTask> validationTasks;
	private final ListProperty<LTLPatternItem> ltlPatterns;
	private final ListProperty<SimulationModel> simulations;
	private final ObjectProperty<Path> visBVisualisation;
	private final ListProperty<String> historyChartItems;

	@JsonIgnore
	private PatternManager patternManager = new PatternManager();
	@JsonIgnore
	private final CachedEditorState cachedEditorState;
	@JsonIgnore
	private final BooleanProperty changed = new SimpleBooleanProperty(false);

	public Machine(final String name, final String description, final Path location) {
		this(name, description, location, null);
	}

	@JsonCreator
	public Machine(
		@JsonProperty("name") final String name,
		@JsonProperty("description") final String description,
		@JsonProperty("location") final Path location,
		@JsonProperty("lastUsedPreferenceName") final String lastUsedPreferenceName
	) {
		this.name = new SimpleStringProperty(this, "name", Objects.requireNonNull(name, "name"));
		this.description = new SimpleStringProperty(this, "description", Objects.requireNonNull(description, "description"));
		this.location = Objects.requireNonNull(location, "location");
		this.lastUsedPreferenceName = new SimpleStringProperty(this, "lastUsedPreferenceName", lastUsedPreferenceName != null && !lastUsedPreferenceName.isEmpty() ? lastUsedPreferenceName : Preference.DEFAULT.getName());

		this.validationTasks = new SimpleListProperty<>(this, "validationTasks", FXCollections.observableArrayList());
		this.ltlPatterns = new SimpleListProperty<>(this, "ltlPatterns", FXCollections.observableArrayList());
		this.simulations = new SimpleListProperty<>(this, "simulations", FXCollections.observableArrayList());
		this.visBVisualisation = new SimpleObjectProperty<>(this, "visBVisualisation", null);
		this.historyChartItems = new SimpleListProperty<>(this, "historyChartItems", FXCollections.observableArrayList());

		this.cachedEditorState = new CachedEditorState();

		this.initListeners();
	}

	private void initListeners() {
		final InvalidationListener changedListener = o -> this.setChanged(true);
		this.nameProperty().addListener(changedListener);
		this.descriptionProperty().addListener(changedListener);
		this.lastUsedPreferenceNameProperty().addListener(changedListener);

		this.getValidationTasks().addListener(changedListener);
		this.getLTLPatterns().addListener(changedListener);
		this.getSimulations().addListener(changedListener);
		this.visBVisualizationProperty().addListener(changedListener);
		this.getHistoryChartItems().addListener(changedListener);
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

	public Path getLocation() {
		return this.location;
	}

	@JsonGetter("validationTasks")
	public ObservableList<IValidationTask> getValidationTasks() {
		return this.validationTasks;
	}

	@JsonSetter("validationTasks")
	private void setValidationTasks(List<IValidationTask> validationTasks) {
		this.getValidationTasks().setAll(validationTasks);
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
	@SuppressWarnings("unchecked")
	public <T extends IValidationTask> ObservableList<T> getValidationTasksByClass(Class<T> clazz) {
		Objects.requireNonNull(clazz, "clazz");
		return (ObservableList<T>) this.getValidationTasksByPredicate(clazz::isInstance);
	}

	@JsonIgnore
	@SuppressWarnings("unchecked")
	public <T extends IValidationTask> ObservableList<T> getValidationTasksByType(ValidationTaskType<T> taskType) {
		Objects.requireNonNull(taskType, "taskType");
		return (ObservableList<T>) this.getValidationTasksByPredicate(vt -> taskType.equals(vt.getTaskType()));
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
		return this.getValidationTasksByType(BuiltinValidationTaskTypes.REPLAY_TRACE);
	}

	public ReadOnlyObjectProperty<MachineCheckingStatus> traceStatusProperty() {
		return new MachineCheckingStatusProperty(this.getTraces());
	}

	@JsonIgnore
	public ObservableList<ModelCheckingItem> getModelCheckingTasks() {
		return this.getValidationTasksByType(BuiltinValidationTaskTypes.MODEL_CHECKING);
	}

	public ReadOnlyObjectProperty<MachineCheckingStatus> modelCheckingStatusProperty() {
		return new MachineCheckingStatusProperty(this.getModelCheckingTasks());
	}

	@JsonIgnore
	public ObservableList<ProofObligationItem> getProofObligationTasks() {
		return this.getValidationTasksByType(BuiltinValidationTaskTypes.PROOF_OBLIGATION);
	}

	@JsonGetter("ltlPatterns")
	public ReadOnlyListProperty<LTLPatternItem> getLTLPatterns() {
		return this.ltlPatterns;
	}

	@JsonSetter("ltlPatterns")
	private void setLTLPatterns(final List<LTLPatternItem> ltlPatterns) {
		this.getLTLPatterns().setAll(ltlPatterns);
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

	@JsonSetter("simulations")
	private void setSimulations(List<SimulationModel> simulations) {
		this.getSimulations().setAll(simulations);
	}

	public ObjectProperty<Path> visBVisualizationProperty() {
		return visBVisualisation;
	}

	@JsonGetter("visBVisualisation")
	public Path getVisBVisualisation() {
		return visBVisualisation.get();
	}

	@JsonSetter("visBVisualisation")
	public void setVisBVisualisation(Path visBVisualisation) {
		this.visBVisualizationProperty().set(visBVisualisation);
	}

	@JsonGetter("historyChartItems")
	public ReadOnlyListProperty<String> getHistoryChartItems() {
		return this.historyChartItems;
	}

	@JsonSetter("historyChartItems")
	public void setHistoryChartItems(List<String> historyChartItems) {
		this.getHistoryChartItems().setAll(historyChartItems);
	}

	@JsonIgnore
	public PatternManager getPatternManager() {
		return patternManager;
	}

	public void clearPatternManager() {
		this.getPatternManager().getPatterns().clear();
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

	public void resetStatus() {
		for (var vt : this.getValidationTasks()) {
			vt.reset();
		}
		this.getLTLPatterns().forEach(LTLPatternItem::reset);
		this.patternManager = new PatternManager();
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
			       .add("name", this.getName())
			       .add("location", this.getLocation())
			       .toString();
	}
}
