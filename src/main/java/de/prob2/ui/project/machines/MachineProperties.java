package de.prob2.ui.project.machines;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSetter;

import de.prob.ltl.parser.pattern.PatternManager;
import de.prob.model.eventb.EventBModel;
import de.prob.model.representation.AbstractModel;
import de.prob2.ui.animation.symbolic.SymbolicAnimationItem;
import de.prob2.ui.animation.symbolic.testcasegeneration.TestCaseGenerationItem;
import de.prob2.ui.animation.tracereplay.ReplayTrace;
import de.prob2.ui.dynamic.DynamicFormulaTask;
import de.prob2.ui.dynamic.dotty.DotFormulaTask;
import de.prob2.ui.dynamic.table.TableFormulaTask;
import de.prob2.ui.simulation.model.SimulationModel;
import de.prob2.ui.simulation.table.SimulationItem;
import de.prob2.ui.verifications.IExecutableItem;
import de.prob2.ui.verifications.IResettable;
import de.prob2.ui.verifications.modelchecking.ModelCheckingItem;
import de.prob2.ui.verifications.po.ProofObligationItem;
import de.prob2.ui.verifications.po.SavedProofObligationItem;
import de.prob2.ui.verifications.symbolicchecking.SymbolicCheckingFormulaItem;
import de.prob2.ui.verifications.temporal.TemporalFormulaItem;
import de.prob2.ui.verifications.temporal.ltl.patterns.LTLPatternItem;
import de.prob2.ui.verifications.type.BuiltinValidationTaskTypes;
import de.prob2.ui.verifications.type.ValidationTaskType;
import de.prob2.ui.vomanager.IValidationTask;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.MapProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyListProperty;
import javafx.beans.property.ReadOnlyMapProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleMapProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

import static de.prob2.ui.project.machines.MachineCheckingStatus.combineMachineCheckingStatus;

@JsonPropertyOrder({
	"validationTasks",
	"ltlPatterns",
	"symbolicAnimationFormulas",
	"testCases",
	"proofObligationItems",
	"simulations",
	"visBVisualisation",
	"historyChartItems",
})
public final class MachineProperties {

	private final ReadOnlyListProperty<IValidationTask<?>> validationTasks;
	@JsonIgnore
	private final Map<ValidationTaskType<? extends IExecutableItem>, ObjectProperty<MachineCheckingStatus>> statusProperties;
	private final ListProperty<LTLPatternItem> ltlPatterns;
	/**
	 * View of all proof obligation items.
	 * {@link MachineProperties#proofObligationItemsWithIds} only saves POs with ids.
	 */
	@JsonIgnore
	private final ListProperty<ProofObligationItem> allProofObligationItems;
	/**
	 * All POs with ids.
	 */
	private final ListProperty<SavedProofObligationItem> proofObligationItemsWithIds;
	private final ListProperty<SymbolicAnimationItem> symbolicAnimationFormulas;
	private final ListProperty<TestCaseGenerationItem> testCases;
	private final ListProperty<SimulationModel> simulations;
	private final ObjectProperty<Path> visBVisualisation;
	private final ListProperty<String> historyChartItems;
	// dynamically collected from individual lists
	@JsonIgnore
	private final MapProperty<String, IValidationTask<?>> validationTasksOld;
	@JsonIgnore
	private final ListChangeListener<IValidationTask<?>> validationTasksOldListener;

	@JsonIgnore
	private final BooleanProperty changed = new SimpleBooleanProperty(false);

	@JsonIgnore
	private PatternManager patternManager = new PatternManager();

	public MachineProperties() {
		this.validationTasks = new SimpleListProperty<>(this, "validationTasks", FXCollections.observableArrayList());
		this.statusProperties = new HashMap<>();

		this.ltlPatterns = new SimpleListProperty<>(this, "ltlPatterns", FXCollections.observableArrayList());
		this.symbolicAnimationFormulas = new SimpleListProperty<>(this, "symbolicAnimationFormulas", FXCollections.observableArrayList());

		this.testCases = new SimpleListProperty<>(this, "testCases", FXCollections.observableArrayList());
		this.allProofObligationItems = new SimpleListProperty<>(this, "allProofObligationItems", FXCollections.observableArrayList());
		this.proofObligationItemsWithIds = new SimpleListProperty<>(this, "proofObligationItemsWithIds", FXCollections.observableArrayList());
		this.simulations = new SimpleListProperty<>(this, "simulations", FXCollections.observableArrayList());
		this.visBVisualisation = new SimpleObjectProperty<>(this, "visBVisualisation", null);
		this.historyChartItems = new SimpleListProperty<>(this, "historyChartItems", FXCollections.observableArrayList());

		this.validationTasksOld = new SimpleMapProperty<>(this, "validationTasks", FXCollections.observableHashMap());
		this.validationTasksOldListener = change -> {
			while (change.next()) {
				for (final IValidationTask<?> vt : change.getRemoved()) {
					this.getValidationTasksOld().remove(vt.getId(), vt);
				}
				for (final IValidationTask<?> vt : change.getAddedSubList()) {
					if (vt.getId() != null) {
						this.getValidationTasksOld().put(vt.getId(), vt);
					}
				}
			}
		};

		this.initListeners();
	}

	private void addValidationTaskListener(final ObservableList<? extends IValidationTask<?>> tasks) {
		tasks.addListener(this.validationTasksOldListener);
		for (final IValidationTask<?> task : tasks) {
			if (task.getId() != null) {
				this.getValidationTasksOld().put(task.getId(), task);
			}
		}
	}

	private void removeValidationTaskListener(final ObservableList<? extends IValidationTask<?>> tasks) {
		tasks.removeListener(this.validationTasksOldListener);
		for (final IValidationTask<?> task : tasks) {
			if (task.getId() != null) {
				this.getValidationTasksOld().remove(task.getId(), task);
			}
		}
	}

	@JsonIgnore
	public ReadOnlyListProperty<IValidationTask<?>> getValidationTasks() {
		return this.validationTasks;
	}

	@JsonGetter("validationTasks")
	private List<IValidationTask<?>> getValidationTasksForSerialization() {
		return this.getValidationTasks();
	}

	@JsonSetter("validationTasks")
	private void setValidationTasksForDeserialization(List<IValidationTask<?>> validationTasks) {
		this.getValidationTasks().setAll(validationTasks);
	}

	@JsonIgnore
	private ObservableList<IValidationTask<?>> getValidationTasksByPredicate(Predicate<IValidationTask<?>> predicate) {
		return this.getValidationTasks().filtered(predicate);
	}

	@JsonIgnore
	public ObservableList<IValidationTask<?>> getValidationTasksWithId() {
		return this.getValidationTasksByPredicate(vt -> vt.getId() != null);
	}

	@JsonIgnore
	@SuppressWarnings("unchecked")
	public <T extends IValidationTask<T>> ObservableList<T> getValidationTasksByType(ValidationTaskType<T> taskType) {
		Objects.requireNonNull(taskType, "taskType");
		return (ObservableList<T>) this.getValidationTasksByPredicate(vt -> taskType.equals(vt.getTaskType()));
	}

	@JsonIgnore
	@SuppressWarnings("unchecked")
	public <T extends DynamicFormulaTask<T>> ObservableList<T> getDynamicFormulaTasksByCommand(ValidationTaskType<T> taskType, String command) {
		Objects.requireNonNull(taskType, "taskType");
		Objects.requireNonNull(command, "command");
		// casting shenanigans to make java's type inference happy
		return (ObservableList<T>) (ObservableList<?>) this.getValidationTasksByPredicate(vt -> taskType.equals(vt.getTaskType()) && command.equals(((DynamicFormulaTask<T>) vt).getCommandType()));
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
		Set<String> ids = this.getValidationTasksWithId().stream().map(IValidationTask::getId).collect(Collectors.toSet());
		ids.addAll(this.getValidationTasksOld().keySet());
		return ids;
	}

	private ObjectProperty<MachineCheckingStatus> createStatusProperty(ValidationTaskType<? extends IExecutableItem> taskType) {
		ObservableList<? extends IExecutableItem> items = this.getValidationTasksByType(taskType);
		ObjectProperty<MachineCheckingStatus> p = new SimpleObjectProperty<>();
		addCheckingStatusListener(items, p);
		return p;
	}

	@JsonIgnore
	public <T extends IValidationTask<T> & IExecutableItem> ReadOnlyObjectProperty<MachineCheckingStatus> getCheckingStatusByType(ValidationTaskType<T> taskType) {
		return this.statusProperties.computeIfAbsent(
			Objects.requireNonNull(taskType, "taskType"),
			this::createStatusProperty
		);
	}

	public void addValidationTask(IValidationTask<?> validationTask) {
		this.getValidationTasks().add(Objects.requireNonNull(validationTask, "validationTask"));
	}

	public void removeValidationTask(IValidationTask<?> validationTask) {
		this.getValidationTasks().remove(Objects.requireNonNull(validationTask, "validationTask"));
	}

	public void replaceValidationTask(IValidationTask<?> oldValidationTask, IValidationTask<?> newValidationTask) {
		Objects.requireNonNull(oldValidationTask, "oldValidationTask");
		Objects.requireNonNull(newValidationTask, "newValidationTask");
		int index = this.getValidationTasks().indexOf(oldValidationTask);
		if (index < 0) {
			throw new IllegalArgumentException("oldValidationTask not found");
		}

		this.getValidationTasks().set(index, newValidationTask);
	}

	@JsonIgnore
	public ObservableList<TemporalFormulaItem> getTemporalFormulas() {
		return this.getValidationTasksByType(BuiltinValidationTaskTypes.TEMPORAL);
	}

	public ReadOnlyObjectProperty<MachineCheckingStatus> temporalStatusProperty() {
		return this.getCheckingStatusByType(BuiltinValidationTaskTypes.TEMPORAL);
	}

	@JsonIgnore
	public ObservableList<SymbolicCheckingFormulaItem> getSymbolicCheckingFormulas() {
		return this.getValidationTasksByType(BuiltinValidationTaskTypes.SYMBOLIC);
	}

	public ReadOnlyObjectProperty<MachineCheckingStatus> symbolicCheckingStatusProperty() {
		return this.getCheckingStatusByType(BuiltinValidationTaskTypes.SYMBOLIC);
	}

	@JsonIgnore
	public ObservableList<ReplayTrace> getTraces() {
		return this.getValidationTasksByType(BuiltinValidationTaskTypes.REPLAY_TRACE);
	}

	public ReadOnlyObjectProperty<MachineCheckingStatus> traceStatusProperty() {
		return this.getCheckingStatusByType(BuiltinValidationTaskTypes.REPLAY_TRACE);
	}

	@JsonIgnore
	public ObservableList<ModelCheckingItem> getModelCheckingTasks() {
		return this.getValidationTasksByType(BuiltinValidationTaskTypes.MODEL_CHECKING);
	}

	public ReadOnlyObjectProperty<MachineCheckingStatus> modelCheckingStatusProperty() {
		return this.getCheckingStatusByType(BuiltinValidationTaskTypes.MODEL_CHECKING);
	}

	@JsonIgnore
	public ObservableList<DotFormulaTask> getDotFormulaTasksByCommand(String command) {
		return this.getDynamicFormulaTasksByCommand(BuiltinValidationTaskTypes.DOT_FORMULA, command);
	}

	@JsonIgnore
	public ObservableList<TableFormulaTask> getTableFormulaTasksByCommand(String command) {
		return this.getDynamicFormulaTasksByCommand(BuiltinValidationTaskTypes.TABLE_FORMULA, command);
	}

	@JsonIgnore
	public ReadOnlyMapProperty<String, IValidationTask<?>> getValidationTasksOld() {
		return this.validationTasksOld;
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

	@JsonIgnore
	public ReadOnlyListProperty<ProofObligationItem> getAllProofObligationItems() {
		return this.allProofObligationItems;
	}

	@JsonGetter("proofObligationItems")
	private ReadOnlyListProperty<SavedProofObligationItem> getProofObligationItemsWithIds() {
		return this.proofObligationItemsWithIds;
	}

	@JsonSetter("proofObligationItems")
	private void setProofObligationItemsWithIds(final List<SavedProofObligationItem> proofObligationItems) {
		// After setting proofObligationItems,
		// allProofObligationItems must be updated manually using updateAllProofObligationsFromModel.
		this.getProofObligationItemsWithIds().setAll(proofObligationItems);
	}

	public void updateAllProofObligationsFromModel(final AbstractModel model) {
		// TODO: Does not yet work with .eventb files
		if (!(model instanceof EventBModel) || ((EventBModel) model).getTopLevelMachine() == null) {
			return;
		}

		// Save the previous POs and allow lookup by name.
		Map<String, SavedProofObligationItem> previousPOsByName = this.getProofObligationItemsWithIds().stream()
			                                                          .collect(Collectors.toMap(SavedProofObligationItem::getName, x -> x));

		// Read the current POs from the model.
		List<ProofObligationItem> proofObligations = ((EventBModel) model).getTopLevelMachine()
			                                             .getProofs()
			                                             .stream()
			                                             .map(ProofObligationItem::new)
			                                             .collect(Collectors.toList());

		// Copy any PO validation task IDs from the previous POs.
		// This also removes all POs from previousPOsByName that have a corresponding current PO,
		// leaving only the POs that no longer exist in the model.
		for (ListIterator<ProofObligationItem> iterator = proofObligations.listIterator(); iterator.hasNext(); ) {
			final ProofObligationItem po = iterator.next();
			final SavedProofObligationItem previousPO = previousPOsByName.remove(po.getName());
			if (previousPO != null) {
				iterator.set(po.withId(previousPO.getId()));
			}
		}

		// Look for removed POs that have an ID and keep them,
		// so that POs for which the user assigned an ID don't silently disappear.
		previousPOsByName.values().stream()
			.filter(po -> po.getId() != null)
			.sorted(Comparator.comparing(SavedProofObligationItem::getName))
			.map(ProofObligationItem::new)
			.collect(Collectors.toCollection(() -> proofObligations));

		// Store the updated POs in the machine.
		this.getAllProofObligationItems().setAll(proofObligations);
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
		// TODO: remove this for all validation tasks
		this.getLTLPatterns().addListener(changedListener);
		this.getSymbolicAnimationFormulas().addListener(changedListener);
		this.getTestCases().addListener(changedListener);
		this.getAllProofObligationItems().addListener((o, from, to) -> {
			// Update the saved POs whenever the real PO list changes.
			final List<SavedProofObligationItem> updatedSavedPOs = to.stream()
				                                                       .filter(po -> po.getId() != null)
				                                                       .map(SavedProofObligationItem::new)
				                                                       .collect(Collectors.toList());

			// Avoid marking the machine as unsaved if the POs didn't actually change.
			if (!this.getProofObligationItemsWithIds().equals(updatedSavedPOs)) {
				this.setProofObligationItemsWithIds(updatedSavedPOs);
			}
		});
		this.getProofObligationItemsWithIds().addListener(changedListener);
		this.getSimulations().addListener(changedListener);
		this.visBVisualizationProperty().addListener(changedListener);
		this.getHistoryChartItems().addListener(changedListener);

		// Collect all validation tasks that have a non-null ID
		this.addValidationTaskListener(this.getValidationTasks());
		// TODO: remove this
		this.addValidationTaskListener(this.getAllProofObligationItems());
	}

	private void addCheckingStatusListener(final ObservableList<? extends IExecutableItem> items, final ObjectProperty<MachineCheckingStatus> statusProperty) {
		final InvalidationListener updateListener = o -> Platform.runLater(() -> statusProperty.set(combineMachineCheckingStatus(items)));
		items.addListener((ListChangeListener<IExecutableItem>) change -> {
			while (change.next()) {
				change.getRemoved().forEach(item -> {
					item.selectedProperty().removeListener(updateListener);
					item.checkedProperty().removeListener(updateListener);
				});
				change.getAddedSubList().forEach(item -> {
					item.selectedProperty().addListener(updateListener);
					item.checkedProperty().addListener(updateListener);
				});
			}
			updateListener.invalidated(null);
		});
		items.forEach(item -> {
			item.selectedProperty().addListener(updateListener);
			item.checkedProperty().addListener(updateListener);
		});
		updateListener.invalidated(null);
	}

	public void resetStatus() {
		for (var vt : this.getValidationTasks()) {
			if (vt instanceof IResettable r) {
				r.reset();
			}
		}
		this.getSymbolicAnimationFormulas().forEach(IExecutableItem::reset);
		this.getTestCases().forEach(IExecutableItem::reset);
		this.getLTLPatterns().forEach(LTLPatternItem::reset);
		this.patternManager = new PatternManager();
	}
}
