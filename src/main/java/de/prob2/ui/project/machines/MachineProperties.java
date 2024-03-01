package de.prob2.ui.project.machines;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleMapProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;

import se.sawano.java.text.AlphanumericComparator;

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

	private final Map<ValidationTaskType<?>, ListProperty<? extends IValidationTask<?>>> validationTasks;
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
		this.validationTasks = new HashMap<>();
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

	private <T extends IValidationTask<T>> void addValidationTaskListener(final ObservableList<T> tasks) {
		tasks.addListener(this.validationTasksOldListener);
		for (final IValidationTask<T> task : tasks) {
			if (task.getId() != null) {
				this.validationTasksOldProperty().put(task.getId(), task);
			}
		}
	}

	private <T extends IValidationTask<T>> void removeValidationTaskListener(final ObservableList<T> tasks) {
		tasks.removeListener(this.validationTasksOldListener);
		for (final IValidationTask<T> task : tasks) {
			if (task.getId() != null) {
				this.validationTasksOldProperty().remove(task.getId(), task);
			}
		}
	}

	@JsonGetter("validationTasks")
	private List<IValidationTask<?>> getValidationTasks() {
		final Comparator<IValidationTask<?>> typeComparator = Comparator.<IValidationTask<?>, ValidationTaskType<?>>comparing(IValidationTask::getTaskType)
			                                                      .thenComparing(IValidationTask::getId, Comparator.nullsLast(new AlphanumericComparator(Locale.ROOT)));

		List<IValidationTask<?>> validationTasks = new ArrayList<>();
		for (var values : this.validationTasks.values()) {
			validationTasks.addAll(values);
		}
		validationTasks.sort(typeComparator);
		return validationTasks;
	}

	@JsonSetter("validationTasks")
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void setValidationTasks(List<IValidationTask<?>> validationTasks) {
		this.validationTasks.clear();
		for (IValidationTask<?> vt : Objects.requireNonNull(validationTasks, "validationTasks")) {
			// due to the bounds on IValidationTask<T> we know that vt is an instance of a class T that correctly
			// implements IValidationTask<T> and thus there is a 1:1 correspondence between its TaskType and itself
			this.addValidationTask((IValidationTask) vt);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private <T extends IValidationTask<T>> ListProperty<T> createTaskList(ValidationTaskType<T> taskType) {
		final InvalidationListener changedListener = o -> this.setChanged(true);

		SimpleListProperty<T> p = new SimpleListProperty<>(FXCollections.observableArrayList());
		p.addListener(changedListener);
		p.addListener(this.validationTasksOldListener);
		if (taskType.isExecutableItem()) {
			addCheckingStatusListener((ReadOnlyListProperty<? extends IExecutableItem>) p, this.getCheckingStatusByType((ValidationTaskType) taskType));
		}

		return p;
	}

	@JsonIgnore
	@SuppressWarnings({ "unchecked" })
	public <T extends IValidationTask<T>> ListProperty<T> getValidationTasksByType(ValidationTaskType<T> taskType) {
		return (ListProperty<T>) this.validationTasks.computeIfAbsent(
			Objects.requireNonNull(taskType, "taskType"),
			this::createTaskList
		);
	}

	@JsonIgnore
	public <T extends IValidationTask<T> & IExecutableItem> ObjectProperty<MachineCheckingStatus> getCheckingStatusByType(ValidationTaskType<T> taskType) {
		return this.statusProperties.computeIfAbsent(
			Objects.requireNonNull(taskType, "taskType"),
			k -> new SimpleObjectProperty<>()
		);
	}

	public <T extends IValidationTask<T>> void addValidationTask(T validationTask) {
		Objects.requireNonNull(validationTask, "validationTask");
		this.getValidationTasksByType(validationTask.getTaskType()).add(validationTask);
	}

	public ListProperty<TemporalFormulaItem> temporalFormulasProperty() {
		return this.getValidationTasksByType(BuiltinValidationTaskTypes.TEMPORAL);
	}

	@JsonIgnore
	public List<TemporalFormulaItem> getTemporalFormulas() {
		return this.temporalFormulasProperty().get();
	}

	public ObjectProperty<MachineCheckingStatus> temporalStatusProperty() {
		return this.getCheckingStatusByType(BuiltinValidationTaskTypes.TEMPORAL);
	}

	public ObjectProperty<MachineCheckingStatus> traceReplayStatusProperty() {
		return trace.statusProperty();
	}

	@JsonIgnore
	public MachineCheckingStatus getTraceReplayStatus() {
		return traceReplayStatusProperty().get();
	}

	public ObjectProperty<MachineCheckingStatus> symbolicCheckingStatusProperty() {
		return this.symbolic.statusProperty();
	}

	@JsonIgnore
	public MachineCheckingStatus getSymbolicCheckingStatus() {
		return this.symbolicCheckingStatusProperty().get();
	}

	public ObjectProperty<MachineCheckingStatus> modelcheckingStatusProperty() {
		return this.modelchecking.statusProperty();
	}

	@JsonIgnore
	public MachineCheckingStatus getModelcheckingStatus() {
		return this.modelcheckingStatusProperty().get();
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

		addCheckingStatusListener(this.symbolicCheckingFormulasProperty(), this.symbolicCheckingStatusProperty());
		addCheckingStatusListener(this.tracesProperty(), this.traceReplayStatusProperty());
		addCheckingStatusListener(this.modelcheckingItemsProperty(), this.modelcheckingStatusProperty());

		// Collect all validation tasks that have a non-null ID
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

	private void addCheckingStatusListener(final ReadOnlyListProperty<? extends IExecutableItem> items, final ObjectProperty<MachineCheckingStatus> statusProperty) {
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
			if (vt instanceof IExecutableItem executableItem) {
				executableItem.reset();
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

	@JsonIgnore
	public Set<String> getValidationTaskIds() {
		Set<String> ids = new HashSet<>(this.validationTasksOldProperty().get().keySet());
		for (var vt : this.getValidationTasks()) {
			var id = vt.getId();
			if (id != null) {
				ids.add(id);
			}
		}
		return ids;
	}
}
