package de.prob2.ui.project.machines;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSetter;

import de.prob.ltl.parser.pattern.PatternManager;
import de.prob2.ui.animation.symbolic.SymbolicAnimationItem;
import de.prob2.ui.animation.symbolic.testcasegeneration.TestCaseGenerationItem;
import de.prob2.ui.animation.tracereplay.ReplayTrace;
import de.prob2.ui.dynamic.DynamicFormulaTask;
import de.prob2.ui.dynamic.dotty.DotFormulaTask;
import de.prob2.ui.dynamic.plantuml.PlantUmlFormulaTask;
import de.prob2.ui.dynamic.table.TableFormulaTask;
import de.prob2.ui.simulation.model.SimulationModel;
import de.prob2.ui.simulation.table.SimulationItem;
import de.prob2.ui.verifications.IExecutableItem;
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
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

@JsonPropertyOrder({
	"validationTasks",
	"ltlPatterns",
	"symbolicAnimationFormulas",
	"testCases",
	"simulations",
	"visBVisualisation",
	"historyChartItems",
})
public final class MachineProperties {
	private final ReadOnlyListProperty<IValidationTask> validationTasks;
	private final ListProperty<LTLPatternItem> ltlPatterns;
	private final ListProperty<SymbolicAnimationItem> symbolicAnimationFormulas;
	private final ListProperty<TestCaseGenerationItem> testCases;
	private final ListProperty<SimulationModel> simulations;
	private final ObjectProperty<Path> visBVisualisation;
	private final ListProperty<String> historyChartItems;

	@JsonIgnore
	private final BooleanProperty changed = new SimpleBooleanProperty(false);

	@JsonIgnore
	private PatternManager patternManager = new PatternManager();

	public MachineProperties() {
		this.validationTasks = new SimpleListProperty<>(this, "validationTasks", FXCollections.observableArrayList());

		this.ltlPatterns = new SimpleListProperty<>(this, "ltlPatterns", FXCollections.observableArrayList());
		this.symbolicAnimationFormulas = new SimpleListProperty<>(this, "symbolicAnimationFormulas", FXCollections.observableArrayList());

		this.testCases = new SimpleListProperty<>(this, "testCases", FXCollections.observableArrayList());
		this.simulations = new SimpleListProperty<>(this, "simulations", FXCollections.observableArrayList());
		this.visBVisualisation = new SimpleObjectProperty<>(this, "visBVisualisation", null);
		this.historyChartItems = new SimpleListProperty<>(this, "historyChartItems", FXCollections.observableArrayList());

		this.initListeners();
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
	public <T extends DynamicFormulaTask> ObservableList<T> getDynamicFormulaTasksByCommand(ValidationTaskType<T> taskType, String command) {
		Objects.requireNonNull(taskType, "taskType");
		Objects.requireNonNull(command, "command");
		// casting shenanigans to make java's type inference happy
		return (ObservableList<T>) (ObservableList<?>) this.getValidationTasksByPredicate(vt -> taskType.equals(vt.getTaskType()) && command.equals(((DynamicFormulaTask) vt).getCommandType()));
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
		return this.getValidationTasksByType(BuiltinValidationTaskTypes.SYMBOLIC);
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
	public ObservableList<DotFormulaTask> getDotFormulaTasksByCommand(String command) {
		return this.getDynamicFormulaTasksByCommand(BuiltinValidationTaskTypes.DOT_FORMULA, command);
	}

	@JsonIgnore
	public ObservableList<PlantUmlFormulaTask> getPlantUmlFormulaTasksByCommand(String command) {
		return this.getDynamicFormulaTasksByCommand(BuiltinValidationTaskTypes.PLANTUML_FORMULA, command);
	}

	@JsonIgnore
	public ObservableList<TableFormulaTask> getTableFormulaTasksByCommand(String command) {
		return this.getDynamicFormulaTasksByCommand(BuiltinValidationTaskTypes.TABLE_FORMULA, command);
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

	@JsonGetter("symbolicAnimationFormulas")
	public ReadOnlyListProperty<SymbolicAnimationItem> getSymbolicAnimationFormulas() {
		return this.symbolicAnimationFormulas;
	}

	@JsonSetter("symbolicAnimationFormulas")
	private void setSymbolicAnimationFormulas(final List<SymbolicAnimationItem> symbolicAnimationFormulas) {
		this.getSymbolicAnimationFormulas().setAll(symbolicAnimationFormulas);
	}

	@JsonGetter("testCases")
	public ReadOnlyListProperty<TestCaseGenerationItem> getTestCases() {
		return this.testCases;
	}

	@JsonSetter("testCases")
	private void setTestCases(final List<TestCaseGenerationItem> testCases) {
		this.getTestCases().setAll(testCases);
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

	public BooleanProperty changedProperty() {
		return this.changed;
	}

	@JsonIgnore
	public boolean isChanged() {
		return this.changedProperty().get();
	}

	@JsonIgnore
	public void setChanged(final boolean changed) {
		this.changedProperty().set(changed);
	}

	@JsonIgnore
	public PatternManager getPatternManager() {
		return patternManager;
	}

	public void clearPatternManager() {
		this.getPatternManager().getPatterns().clear();
	}

	private void initListeners() {
		final InvalidationListener changedListener = o -> this.setChanged(true);
		this.getValidationTasks().addListener(changedListener);
		this.getLTLPatterns().addListener(changedListener);
		this.getSymbolicAnimationFormulas().addListener(changedListener);
		this.getTestCases().addListener(changedListener);
		this.getSimulations().addListener(changedListener);
		this.visBVisualizationProperty().addListener(changedListener);
		this.getHistoryChartItems().addListener(changedListener);
	}

	public void resetStatus() {
		for (var vt : this.getValidationTasks()) {
			vt.reset();
		}
		this.getSymbolicAnimationFormulas().forEach(IExecutableItem::reset);
		this.getTestCases().forEach(IExecutableItem::reset);
		this.getLTLPatterns().forEach(LTLPatternItem::reset);
		this.patternManager = new PatternManager();
	}
}
