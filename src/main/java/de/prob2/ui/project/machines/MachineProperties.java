package de.prob2.ui.project.machines;

import java.nio.file.Path;
import java.util.ArrayList;
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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSetter;

import de.prob.ltl.parser.pattern.PatternManager;
import de.prob.model.eventb.EventBModel;
import de.prob.model.representation.AbstractModel;
import de.prob2.ui.animation.symbolic.SymbolicAnimationItem;
import de.prob2.ui.animation.symbolic.testcasegeneration.TestCaseGenerationItem;
import de.prob2.ui.animation.tracereplay.ReplayTrace;
import de.prob2.ui.dynamic.DynamicCommandFormulaItem;
import de.prob2.ui.simulation.model.SimulationModel;
import de.prob2.ui.simulation.table.SimulationItem;
import de.prob2.ui.verifications.IExecutableItem;
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
import javafx.collections.ObservableMap;
import javafx.collections.transformation.FilteredList;

import static de.prob2.ui.project.machines.MachineCheckingStatus.combineMachineCheckingStatus;

@JsonPropertyOrder({
	"validationTasks",
	"ltlPatterns",
	"symbolicCheckingFormulas",
	"symbolicAnimationFormulas",
	"testCases",
	"traces",
	"modelcheckingItems",
	"proofObligationItems",
	"simulations",
	"visBVisualisation",
	"historyChartItems",
	"dotVisualizationItems",
	"tableVisualizationItems"
})
public final class MachineProperties {

	private final ReadOnlyListProperty<IValidationTask<?>> validationTasks;
	@JsonIgnore
	private final Map<ValidationTaskType<? extends IExecutableItem>, ObjectProperty<MachineCheckingStatus>> statusProperties;
	private final ListProperty<LTLPatternItem> ltlPatterns;
	private final CheckingProperty<SymbolicCheckingFormulaItem> symbolic;
	private final ListProperty<SymbolicAnimationItem> symbolicAnimationFormulas;
	private final ListProperty<TestCaseGenerationItem> testCases;
	private final CheckingProperty<ReplayTrace> trace;
	private final CheckingProperty<ModelCheckingItem> modelchecking;
	@JsonIgnore // Saved as proofObligationItems instead
	private final ListProperty<ProofObligationItem> allProofObligationItems;
	// Contains only proof obligations that have an ID.
	private final ListProperty<SavedProofObligationItem> proofObligationItems;
	private final ListProperty<SimulationModel> simulations;
	private final ObjectProperty<Path> visBVisualisation;
	private final ListProperty<String> historyChartItems;
	private final MapProperty<String, ListProperty<DynamicCommandFormulaItem>> dotVisualizationItems;
	private final MapProperty<String, ListProperty<DynamicCommandFormulaItem>> tableVisualizationItems;
	// dynamically collected from individual lists
	@JsonIgnore
	private final MapProperty<String, IValidationTask<?>> validationTasksOld;
	private final ListChangeListener<IValidationTask<?>> validationTasksOldListener;

	@JsonIgnore
	private final BooleanProperty changed = new SimpleBooleanProperty(false);

	@JsonIgnore
	private PatternManager patternManager = new PatternManager();

	public MachineProperties() {
		this.validationTasks = new SimpleListProperty<>(this, "validationTasks", FXCollections.observableArrayList());
		this.statusProperties = new HashMap<>();

		this.symbolic = new CheckingProperty<>(new SimpleObjectProperty<>(this, "symbolicCheckingStatus", new MachineCheckingStatus()), new SimpleListProperty<>(this, "symbolicCheckingFormulas", FXCollections.observableArrayList()));
		this.modelchecking = new CheckingProperty<>(new SimpleObjectProperty<>(this, "modelcheckingStatus", new MachineCheckingStatus()), new SimpleListProperty<>(this, "modelcheckingItems", FXCollections.observableArrayList()));
		this.trace = new CheckingProperty<>(new SimpleObjectProperty<>(this, "traceReplayStatus", new MachineCheckingStatus()), new SimpleListProperty<>(this, "traces", FXCollections.observableArrayList()));

		this.ltlPatterns = new SimpleListProperty<>(this, "ltlPatterns", FXCollections.observableArrayList());
		this.symbolicAnimationFormulas = new SimpleListProperty<>(this, "symbolicAnimationFormulas", FXCollections.observableArrayList());

		this.testCases = new SimpleListProperty<>(this, "testCases", FXCollections.observableArrayList());
		this.allProofObligationItems = new SimpleListProperty<>(this, "allProofObligationItems", FXCollections.observableArrayList());
		this.proofObligationItems = new SimpleListProperty<>(this, "proofObligationItems", FXCollections.observableArrayList());
		this.simulations = new SimpleListProperty<>(this, "simulations", FXCollections.observableArrayList());
		this.visBVisualisation = new SimpleObjectProperty<>(this, "visBVisualisation", null);
		this.historyChartItems = new SimpleListProperty<>(this, "historyChartItems", FXCollections.observableArrayList());
		this.dotVisualizationItems = new SimpleMapProperty<>(this, "dotVisualizationItems", FXCollections.observableHashMap());
		this.tableVisualizationItems = new SimpleMapProperty<>(this, "tableVisualizationItems", FXCollections.observableHashMap());

		this.validationTasksOld = new SimpleMapProperty<>(this, "validationTasks", FXCollections.observableHashMap());
		this.validationTasksOldListener = change -> {
			while (change.next()) {
				for (final IValidationTask<?> vt : change.getRemoved()) {
					this.validationTasksOldProperty().remove(vt.getId(), vt);
				}
				for (final IValidationTask<?> vt : change.getAddedSubList()) {
					if (vt.getId() != null) {
						this.validationTasksOldProperty().put(vt.getId(), vt);
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
				this.validationTasksOldProperty().put(task.getId(), task);
			}
		}
	}

	private void removeValidationTaskListener(final ObservableList<? extends IValidationTask<?>> tasks) {
		tasks.removeListener(this.validationTasksOldListener);
		for (final IValidationTask<?> task : tasks) {
			if (task.getId() != null) {
				this.validationTasksOldProperty().remove(task.getId(), task);
			}
		}
	}

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
	private FilteredList<IValidationTask<?>> getValidationTasksByPredicate(Predicate<IValidationTask<?>> predicate) {
		return this.getValidationTasks().filtered(predicate);
	}

	@JsonIgnore
	@SuppressWarnings("unchecked")
	public <T extends IValidationTask<T>> FilteredList<T> getValidationTasksByType(ValidationTaskType<T> taskType) {
		Objects.requireNonNull(taskType, "taskType");
		return (FilteredList<T>) this.getValidationTasksByPredicate(vt -> taskType.equals(vt.getTaskType()));
	}

	@JsonIgnore
	public FilteredList<IValidationTask<?>> getValidationTasksWithId() {
		return this.getValidationTasksByPredicate(vt -> vt.getId() != null);
	}

	@JsonIgnore
	public Set<String> getValidationTaskIds() {
		Set<String> ids = this.getValidationTasksWithId().stream().map(IValidationTask::getId).collect(Collectors.toSet());
		ids.addAll(this.validationTasksOldProperty().get().keySet());
		return ids;
	}

	private ObjectProperty<MachineCheckingStatus> createStatusProperty(ValidationTaskType<? extends IExecutableItem> taskType) {
		FilteredList<? extends IExecutableItem> items = this.getValidationTasksByType(taskType);
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

	public <T extends IValidationTask<T>> void addValidationTask(T validationTask) {
		this.getValidationTasks().add(Objects.requireNonNull(validationTask, "validationTask"));
	}

	public <T extends IValidationTask<T>> void removeValidationTask(T validationTask) {
		this.getValidationTasks().remove(Objects.requireNonNull(validationTask, "validationTask"));
	}

	public <T extends IValidationTask<T>> void replaceValidationTask(T oldValidationTask, T newValidationTask) {
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

	public ReadOnlyObjectProperty<MachineCheckingStatus> traceReplayStatusProperty() {
		return trace.statusProperty();
	}

	public ReadOnlyObjectProperty<MachineCheckingStatus> symbolicCheckingStatusProperty() {
		return this.symbolic.statusProperty();
	}

	public ReadOnlyObjectProperty<MachineCheckingStatus> modelcheckingStatusProperty() {
		return this.modelchecking.statusProperty();
	}

	public ReadOnlyMapProperty<String, IValidationTask<?>> validationTasksOldProperty() {
		return this.validationTasksOld;
	}

	public ListProperty<LTLPatternItem> ltlPatternsProperty() {
		return ltlPatterns;
	}

	@JsonProperty("ltlPatterns")
	public List<LTLPatternItem> getLTLPatterns() {
		return ltlPatternsProperty().get();
	}

	@JsonProperty("ltlPatterns")
	private void setLTLPatterns(final List<LTLPatternItem> ltlPatterns) {
		this.ltlPatternsProperty().setAll(ltlPatterns);
	}

	public ListProperty<SymbolicCheckingFormulaItem> symbolicCheckingFormulasProperty() {
		return this.symbolic.itemProperty();
	}

	public List<SymbolicCheckingFormulaItem> getSymbolicCheckingFormulas() {
		return this.symbolicCheckingFormulasProperty().get();
	}

	@JsonProperty
	private void setSymbolicCheckingFormulas(final List<SymbolicCheckingFormulaItem> symbolicCheckingFormulas) {
		this.symbolicCheckingFormulasProperty().setAll(symbolicCheckingFormulas);
	}

	public ListProperty<SymbolicAnimationItem> symbolicAnimationFormulasProperty() {
		return symbolicAnimationFormulas;
	}

	public List<SymbolicAnimationItem> getSymbolicAnimationFormulas() {
		return symbolicAnimationFormulas.get();
	}

	@JsonProperty
	private void setSymbolicAnimationFormulas(final List<SymbolicAnimationItem> symbolicAnimationFormulas) {
		this.symbolicAnimationFormulasProperty().setAll(symbolicAnimationFormulas);
	}

	public ListProperty<TestCaseGenerationItem> testCasesProperty() {
		return testCases;
	}

	public List<TestCaseGenerationItem> getTestCases() {
		return testCases.get();
	}

	@JsonProperty
	private void setTestCases(final List<TestCaseGenerationItem> testCases) {
		this.testCasesProperty().setAll(testCases);
	}

	public ListProperty<ModelCheckingItem> modelcheckingItemsProperty() {
		return this.modelchecking.itemProperty();
	}

	public List<ModelCheckingItem> getModelcheckingItems() {
		return this.modelcheckingItemsProperty().get();
	}

	@JsonProperty
	private void setModelcheckingItems(final List<ModelCheckingItem> modelcheckingItems) {
		this.modelcheckingItemsProperty().setAll(modelcheckingItems);
	}

	public ListProperty<ProofObligationItem> allProofObligationItemsProperty() {
		return allProofObligationItems;
	}

	public List<ProofObligationItem> getAllProofObligationItems() {
		return allProofObligationItems.get();
	}

	public ListProperty<SavedProofObligationItem> proofObligationItemsProperty() {
		return this.proofObligationItems;
	}

	public List<SavedProofObligationItem> getProofObligationItems() {
		return this.proofObligationItemsProperty().get();
	}

	// After setting proofObligationItems,
	// allProofObligationItems must be updated manually using updateAllProofObligationsFromModel.
	@JsonProperty
	private void setProofObligationItems(final List<SavedProofObligationItem> proofObligationItems) {
		this.proofObligationItemsProperty().setAll(proofObligationItems);
	}

	public void updateAllProofObligationsFromModel(final AbstractModel model) {
		// TODO: Does not yet work with .eventb files
		if (!(model instanceof EventBModel) || ((EventBModel) model).getTopLevelMachine() == null) {
			return;
		}

		// Save the previous POs and allow lookup by name.
		Map<String, SavedProofObligationItem> previousPOsByName = this.getProofObligationItems().stream()
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
		this.allProofObligationItemsProperty().setAll(proofObligations);
	}

	public ListProperty<ReplayTrace> tracesProperty() {
		return this.trace.itemProperty();
	}

	public ObservableList<ReplayTrace> getTraces() {
		return this.tracesProperty().get();
	}

	@JsonProperty
	private void setTraces(final List<ReplayTrace> traces) {
		this.tracesProperty().setAll(traces);
	}

	public ListProperty<SimulationModel> simulationsProperty() {
		return simulations;
	}

	@JsonProperty("simulations")
	public List<SimulationModel> getSimulations() {
		return simulations.get();
	}

	@JsonProperty("simulations")
	public void setSimulations(List<SimulationModel> simulations) {
		this.simulationsProperty().clear();
		this.simulationsProperty().addAll(simulations);
		for (SimulationModel simulationModel : simulations) {
			for (SimulationItem simulationItem : simulationModel.getSimulationItems()) {
				simulationItem.setSimulationModel(simulationModel);
			}
		}
	}

	public ObjectProperty<Path> visBVisualizationProperty() {
		return visBVisualisation;
	}

	public Path getVisBVisualisation() {
		return visBVisualisation.get();
	}

	public void setVisBVisualisation(Path visBVisualisation) {
		this.visBVisualizationProperty().set(visBVisualisation);
	}

	public ListProperty<String> historyChartItemsProperty() {
		return historyChartItems;
	}

	@JsonProperty("historyChartItems")
	public List<String> getHistoryChartItems() {
		return historyChartItems.get();
	}

	@JsonProperty("historyChartItems")
	public void setHistoryChartItems(ArrayList<String> historyChartItems) {
		this.historyChartItems.setValue(FXCollections.observableArrayList(historyChartItems));
	}

	public MapProperty<String, ListProperty<DynamicCommandFormulaItem>> dotVisualizationItemsProperty() {
		return dotVisualizationItems;
	}

	public Map<String, ListProperty<DynamicCommandFormulaItem>> getDotVisualizationItems() {
		return dotVisualizationItems.get();
	}

	@JsonProperty("dotVisualizationItems")
	public void setDotVisualizationItems(Map<String, List<DynamicCommandFormulaItem>> dotVisualizationItems) {
		this.dotVisualizationItems.setValue(convertToObservable(dotVisualizationItems));
	}

	private ObservableMap<String, ListProperty<DynamicCommandFormulaItem>> convertToObservable(Map<String, List<DynamicCommandFormulaItem>> VisualizationItems) {
		ObservableMap<String, ListProperty<DynamicCommandFormulaItem>> map = FXCollections.observableHashMap();
		for (String key : VisualizationItems.keySet()) {
			ObservableList<DynamicCommandFormulaItem> collections = FXCollections.observableArrayList();
			collections.addAll(VisualizationItems.get(key));
			ListProperty<DynamicCommandFormulaItem> listProperty = new SimpleListProperty<>(collections);
			map.put(key, listProperty);
			listProperty.addListener((InvalidationListener) o -> this.setChanged(true));
			this.addValidationTaskListener(listProperty);
		}
		return map;
	}

	public void addDotVisualizationItem(String commandType, DynamicCommandFormulaItem formula) {
		Map<String, ListProperty<DynamicCommandFormulaItem>> map = getDotVisualizationItems();
		if (!map.containsKey(commandType)) {
			addDotVisualizationListProperty(commandType);
		}
		map.get(commandType).add(formula);
	}

	public void removeDotVisualizationItem(String commandType, DynamicCommandFormulaItem formula) {
		Map<String, ListProperty<DynamicCommandFormulaItem>> map = getDotVisualizationItems();
		if (map.containsKey(commandType)) {
			map.get(commandType).remove(formula);
		}
	}

	public void addDotVisualizationListProperty(String commandType) {
		ListProperty<DynamicCommandFormulaItem> listProperty = new SimpleListProperty<>(FXCollections.observableArrayList());
		dotVisualizationItems.put(commandType, listProperty);
		listProperty.addListener((InvalidationListener) o -> this.setChanged(true));
		this.addValidationTaskListener(listProperty);
	}

	public MapProperty<String, ListProperty<DynamicCommandFormulaItem>> tableVisualizationItemsProperty() {
		return tableVisualizationItems;
	}

	public Map<String, ListProperty<DynamicCommandFormulaItem>> getTableVisualizationItems() {
		return tableVisualizationItems.get();
	}

	@JsonProperty("tableVisualizationItems")
	public void setTableVisualizationItems(Map<String, List<DynamicCommandFormulaItem>> tableVisualizationItems) {
		this.tableVisualizationItems.setValue(convertToObservable(tableVisualizationItems));
	}

	public void addTableVisualizationItem(String commandType, DynamicCommandFormulaItem formula) {
		Map<String, ListProperty<DynamicCommandFormulaItem>> map = getTableVisualizationItems();
		if (!map.containsKey(commandType)) {
			addTableVisualizationListProperty(commandType);
		}
		map.get(commandType).add(formula);
	}

	public void removeTableVisualizationItem(String commandType, DynamicCommandFormulaItem formula) {
		Map<String, ListProperty<DynamicCommandFormulaItem>> map = getTableVisualizationItems();
		if (map.containsKey(commandType)) {
			map.get(commandType).remove(formula);
		}
	}

	public void addTableVisualizationListProperty(String commandType) {
		ListProperty<DynamicCommandFormulaItem> listProperty = new SimpleListProperty<>(FXCollections.observableArrayList());
		tableVisualizationItems.put(commandType, listProperty);
		listProperty.addListener((InvalidationListener) o -> this.setChanged(true));
		this.addValidationTaskListener(listProperty);
	}

	public BooleanProperty changedProperty() {
		return this.changed;
	}

	public boolean isChanged() {
		return this.changedProperty().get();
	}

	public void setChanged(final boolean changed) {
		this.changedProperty().set(changed);
	}

	public PatternManager getPatternManager() {
		return patternManager;
	}

	public void clearPatternManager() {
		patternManager.getPatterns().clear();
	}

	private void initListeners() {
		final InvalidationListener changedListener = o -> this.setChanged(true);
		this.getValidationTasks().addListener(changedListener);
		// TODO: remove this for all validation tasks
		this.ltlPatternsProperty().addListener(changedListener);
		this.symbolicCheckingFormulasProperty().addListener(changedListener);
		this.symbolicAnimationFormulasProperty().addListener(changedListener);
		this.testCasesProperty().addListener(changedListener);
		this.tracesProperty().addListener(changedListener);
		this.modelcheckingItemsProperty().addListener(changedListener);
		this.allProofObligationItemsProperty().addListener((o, from, to) -> {
			// Update the saved POs whenever the real PO list changes.
			final List<SavedProofObligationItem> updatedSavedPOs = to.stream()
				                                                       .filter(po -> po.getId() != null)
				                                                       .map(SavedProofObligationItem::new)
				                                                       .collect(Collectors.toList());

			// Avoid marking the machine as unsaved if the POs didn't actually change.
			if (!this.getProofObligationItems().equals(updatedSavedPOs)) {
				this.setProofObligationItems(updatedSavedPOs);
			}
		});
		this.proofObligationItemsProperty().addListener(changedListener);
		this.simulationsProperty().addListener(changedListener);
		this.visBVisualizationProperty().addListener(changedListener);
		this.historyChartItemsProperty().addListener(changedListener);
		this.dotVisualizationItemsProperty().addListener(changedListener);
		this.tableVisualizationItemsProperty().addListener(changedListener);

		// TODO: remove this
		addCheckingStatusListener(this.symbolicCheckingFormulasProperty(), this.symbolic.statusProperty());
		addCheckingStatusListener(this.tracesProperty(), this.trace.statusProperty());
		addCheckingStatusListener(this.modelcheckingItemsProperty(), this.modelchecking.statusProperty());

		// Collect all validation tasks that have a non-null ID
		// TODO: remove this
		this.addValidationTaskListener(this.getValidationTasks());
		this.addValidationTaskListener(this.symbolicCheckingFormulasProperty());
		this.addValidationTaskListener(this.tracesProperty());
		this.addValidationTaskListener(this.modelcheckingItemsProperty());
		this.addValidationTaskListener(this.allProofObligationItemsProperty());

		this.simulationsProperty().addListener((ListChangeListener<SimulationModel>) change -> {
			while (change.next()) {
				for (final SimulationModel simulationModel : change.getRemoved()) {
					this.removeValidationTaskListener(simulationModel.simulationItemsProperty());
				}
				for (final SimulationModel simulationModel : change.getAddedSubList()) {
					this.addValidationTaskListener(simulationModel.simulationItemsProperty());
				}
			}
		});
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
			if (vt instanceof IExecutableItem et) {
				et.reset();
			}
		}
		ltlPatterns.forEach(LTLPatternItem::reset);
		patternManager = new PatternManager();
		symbolicCheckingFormulasProperty().forEach(SymbolicCheckingFormulaItem::reset);
		symbolicAnimationFormulas.forEach(SymbolicAnimationItem::reset);
		simulations.forEach(SimulationModel::reset);
		testCases.forEach(TestCaseGenerationItem::reset);
		tracesProperty().forEach(ReplayTrace::reset);
		modelcheckingItemsProperty().forEach(ModelCheckingItem::reset);
	}
}
