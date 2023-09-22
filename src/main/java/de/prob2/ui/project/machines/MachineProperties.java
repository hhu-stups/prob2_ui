package de.prob2.ui.project.machines;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
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
import de.prob2.ui.vomanager.IValidationTask;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static de.prob2.ui.project.machines.MachineCheckingStatus.combineMachineCheckingStatus;
@JsonPropertyOrder({
	"validationTasks",
	"temporalFormulas",
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
public class MachineProperties {
	@JsonIgnore
	private final ObjectProperty<MachineCheckingStatus> traceReplayStatus = new SimpleObjectProperty<>(this, "traceReplayStatus", new MachineCheckingStatus());
	@JsonIgnore
	private final ObjectProperty<MachineCheckingStatus> temporalStatus = new SimpleObjectProperty<>(this, "temporalStatus", new MachineCheckingStatus());
	@JsonIgnore
	private final ObjectProperty<MachineCheckingStatus> symbolicCheckingStatus = new SimpleObjectProperty<>(this, "symbolicCheckingStatus", new MachineCheckingStatus());
	@JsonIgnore
	private final ObjectProperty<MachineCheckingStatus> modelcheckingStatus = new SimpleObjectProperty<>(this, "modelcheckingStatus", new MachineCheckingStatus());

	private final ListProperty<TemporalFormulaItem> temporalFormulas;
	private final ListProperty<LTLPatternItem> ltlPatterns;
	private final ListProperty<SymbolicCheckingFormulaItem> symbolicCheckingFormulas;
	private final ListProperty<SymbolicAnimationItem> symbolicAnimationFormulas;
	private final ListProperty<TestCaseGenerationItem> testCases;
	private final ListProperty<ReplayTrace> traces;
	private final ListProperty<ModelCheckingItem> modelcheckingItems;
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
	private final MapProperty<String, IValidationTask> validationTasks;
	private final ListChangeListener<IValidationTask> validationTaskListener;

	@JsonIgnore
	private final BooleanProperty changed = new SimpleBooleanProperty(false);

	@JsonIgnore
	private PatternManager patternManager = new PatternManager();


	public MachineProperties() {
		this.temporalFormulas = new SimpleListProperty<>(this, "temporalFormulas", FXCollections.observableArrayList());
		this.ltlPatterns = new SimpleListProperty<>(this, "ltlPatterns", FXCollections.observableArrayList());
		this.symbolicCheckingFormulas = new SimpleListProperty<>(this, "symbolicCheckingFormulas", FXCollections.observableArrayList());
		this.symbolicAnimationFormulas = new SimpleListProperty<>(this, "symbolicAnimationFormulas", FXCollections.observableArrayList());

		this.testCases = new SimpleListProperty<>(this, "testCases", FXCollections.observableArrayList());
		this.traces = new SimpleListProperty<>(this, "traces", FXCollections.observableArrayList());
		this.modelcheckingItems = new SimpleListProperty<>(this, "modelcheckingItems", FXCollections.observableArrayList());
		this.allProofObligationItems = new SimpleListProperty<>(this, "allProofObligationItems", FXCollections.observableArrayList());
		this.proofObligationItems = new SimpleListProperty<>(this, "proofObligationItems", FXCollections.observableArrayList());
		this.simulations = new SimpleListProperty<>(this, "simulations", FXCollections.observableArrayList());
		this.visBVisualisation = new SimpleObjectProperty<>(this, "visBVisualisation", null);
		this.historyChartItems = new SimpleListProperty<>(this, "historyChartItems", FXCollections.observableArrayList());
		this.dotVisualizationItems = new SimpleMapProperty<>(this, "dotVisualizationItems", FXCollections.observableHashMap());
		this.tableVisualizationItems = new SimpleMapProperty<>(this, "tableVisualizationItems", FXCollections.observableHashMap());

		this.validationTasks = new SimpleMapProperty<>(this, "validationTasks", FXCollections.observableHashMap());
		this.validationTaskListener = change -> {
			while (change.next()) {
				for (final IValidationTask vt : change.getRemoved()) {
					this.validationTasks.remove(vt.getId(), vt);
				}
				for (final IValidationTask vt : change.getAddedSubList()) {
					if (vt.getId() != null) {
						this.validationTasks.put(vt.getId(), vt);
					}
				}
			}
		};

		this.initListeners();
	}

	public void addValidationTaskListener(final ObservableList<? extends IValidationTask> tasks) {
		tasks.addListener(this.validationTaskListener);
		for (final IValidationTask task : tasks) {
			if (task.getId() != null) {
				this.validationTasks.put(task.getId(), task);
			}
		}
	}

	public void removeValidationTaskListener(final ObservableList<? extends IValidationTask> tasks) {
		tasks.removeListener(this.validationTaskListener);
		for (final IValidationTask task : tasks) {
			if (task.getId() != null) {
				this.validationTasks.remove(task.getId(), task);
			}
		}
	}
	public ObjectProperty<MachineCheckingStatus> traceReplayStatusProperty() {
		return traceReplayStatus;
	}

	public MachineCheckingStatus getTraceReplayStatus() {
		return traceReplayStatus.get();
	}
	public void setTraceReplayStatus(final MachineCheckingStatus status) {
		this.traceReplayStatusProperty().set(status);
	}

	public ObjectProperty<MachineCheckingStatus> temporalStatusProperty() {
		return this.temporalStatus;
	}

	public MachineCheckingStatus getTemporalStatus() {
		return this.temporalStatusProperty().get();
	}

	public void setTemporalStatus(final MachineCheckingStatus status) {
		this.temporalStatusProperty().set(status);
	}
	public ObjectProperty<MachineCheckingStatus> symbolicCheckingStatusProperty() {
		return this.symbolicCheckingStatus;
	}

	public MachineCheckingStatus getSymbolicCheckingStatus() {
		return this.symbolicCheckingStatusProperty().get();
	}

	public void setSymbolicCheckingStatus(final MachineCheckingStatus status) {
		this.symbolicCheckingStatusProperty().set(status);
	}

	public ObjectProperty<MachineCheckingStatus> modelcheckingStatusProperty() {
		return this.modelcheckingStatus;
	}

	public MachineCheckingStatus getModelcheckingStatus() {
		return this.modelcheckingStatusProperty().get();
	}

	public void setModelcheckingStatus(final MachineCheckingStatus status) {
		this.modelcheckingStatusProperty().set(status);
	}

	public ReadOnlyMapProperty<String, IValidationTask> validationTasksProperty() {
		return this.validationTasks;
	}

	@JsonIgnore
	public ObservableMap<String, IValidationTask> getValidationTasks() {
		return this.validationTasksProperty().get();
	}

	public ListProperty<TemporalFormulaItem> temporalFormulasProperty() {
		return temporalFormulas;
	}

	@JsonProperty("temporalFormulas")
	public List<TemporalFormulaItem> getTemporalFormulas() {
		return temporalFormulasProperty().get();
	}

	@JsonProperty("temporalFormulas")
	private void setTemporalFormulas(final List<TemporalFormulaItem> temporalFormulas) {
		this.temporalFormulasProperty().setAll(temporalFormulas);
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
		return symbolicCheckingFormulas;
	}

	public List<SymbolicCheckingFormulaItem> getSymbolicCheckingFormulas() {
		return symbolicCheckingFormulas.get();
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
		return modelcheckingItems;
	}

	public List<ModelCheckingItem> getModelcheckingItems() {
		return modelcheckingItems.get();
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
		if(!(model instanceof EventBModel) || ((EventBModel) model).getTopLevelMachine() == null) {
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
		for (ListIterator<ProofObligationItem> iterator = proofObligations.listIterator(); iterator.hasNext();) {
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
		return this.traces;
	}

	public ObservableList<ReplayTrace> getTraces() {
		return this.traces.get();
	}

	@JsonProperty
	private void setTraces(final List<ReplayTrace> traces) {
		this.traces.setAll(traces);
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
		for(SimulationModel simulationModel : simulations) {
			for(SimulationItem simulationItem : simulationModel.getSimulationItems()) {
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
		for(String key : VisualizationItems.keySet()) {
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
		if(!map.containsKey(commandType)) {
			addDotVisualizationListProperty(commandType);
		}
		map.get(commandType).add(formula);
	}

	public void removeDotVisualizationItem(String commandType, DynamicCommandFormulaItem formula) {
		Map<String, ListProperty<DynamicCommandFormulaItem>> map = getDotVisualizationItems();
		if(map.containsKey(commandType)) {
			map.get(commandType).remove(formula);
		}
	}

	public void addDotVisualizationListProperty(String commandType) {
		ListProperty<DynamicCommandFormulaItem> listProperty = new SimpleListProperty<>(FXCollections.observableArrayList());
		dotVisualizationItems.put(commandType, listProperty);
		listProperty.addListener((InvalidationListener) o -> this.changed.set(true));
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
		if(!map.containsKey(commandType)) {
			addTableVisualizationListProperty(commandType);
		}
		map.get(commandType).add(formula);
	}

	public void removeTableVisualizationItem(String commandType, DynamicCommandFormulaItem formula) {
		Map<String, ListProperty<DynamicCommandFormulaItem>> map = getTableVisualizationItems();
		if(map.containsKey(commandType)) {
			map.get(commandType).remove(formula);
		}
	}

	public void addTableVisualizationListProperty(String commandType) {
		ListProperty<DynamicCommandFormulaItem> listProperty = new SimpleListProperty<>(FXCollections.observableArrayList());
		tableVisualizationItems.put(commandType, listProperty);
		listProperty.addListener((InvalidationListener) o -> this.changed.set(true));
		this.addValidationTaskListener(listProperty);
	}

	public BooleanProperty changedProperty() {
		return changed;
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


	public void initListeners() {
		final InvalidationListener changedListener = o -> this.setChanged(true);
		this.temporalFormulasProperty().addListener(changedListener);
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

		addCheckingStatusListener(this.temporalFormulasProperty(), this.temporalStatusProperty());
		addCheckingStatusListener(this.symbolicCheckingFormulasProperty(), this.symbolicCheckingStatusProperty());
		addCheckingStatusListener(this.tracesProperty(), this.traceReplayStatusProperty());
		addCheckingStatusListener(this.modelcheckingItemsProperty(), this.modelcheckingStatusProperty());

		// Collect all validation tasks that have a non-null ID
		this.addValidationTaskListener(this.temporalFormulasProperty());
		this.addValidationTaskListener(this.symbolicCheckingFormulasProperty());
		this.addValidationTaskListener(this.tracesProperty());
		this.addValidationTaskListener(this.modelcheckingItemsProperty());
		this.addValidationTaskListener(this.allProofObligationItemsProperty());

		this.simulationsProperty().addListener((ListChangeListener<SimulationModel>)change -> {
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

	public void addCheckingStatusListener(final ReadOnlyListProperty<? extends IExecutableItem> items, final ObjectProperty<MachineCheckingStatus> statusProperty) {
		final InvalidationListener updateListener = o -> Platform.runLater(() -> statusProperty.set(combineMachineCheckingStatus(items)));
		items.addListener((ListChangeListener<IExecutableItem>)change -> {
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
		temporalFormulas.forEach(TemporalFormulaItem::reset);
		ltlPatterns.forEach(LTLPatternItem::reset);
		patternManager = new PatternManager();
		symbolicCheckingFormulas.forEach(SymbolicCheckingFormulaItem::reset);
		symbolicAnimationFormulas.forEach(SymbolicAnimationItem::reset);
		simulations.forEach(SimulationModel::reset);
		testCases.forEach(TestCaseGenerationItem::reset);
		traces.forEach(ReplayTrace::reset);
		modelcheckingItems.forEach(ModelCheckingItem::reset);
	}

}
