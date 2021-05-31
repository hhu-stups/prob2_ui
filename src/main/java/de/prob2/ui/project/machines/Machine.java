package de.prob2.ui.project.machines;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.google.common.io.MoreFiles;

import de.prob.ltl.parser.pattern.PatternManager;
import de.prob.scripting.FactoryProvider;
import de.prob.scripting.ModelFactory;
import de.prob2.ui.animation.symbolic.SymbolicAnimationItem;
import de.prob2.ui.animation.symbolic.testcasegeneration.TestCaseGenerationItem;
import de.prob2.ui.project.preferences.Preference;
import de.prob2.ui.sharedviews.DescriptionView;
import de.prob2.ui.simulation.table.SimulationItem;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.IExecutableItem;
import de.prob2.ui.verifications.ltl.formula.LTLFormulaItem;
import de.prob2.ui.verifications.ltl.patterns.LTLPatternItem;
import de.prob2.ui.verifications.modelchecking.ModelCheckingItem;
import de.prob2.ui.verifications.symbolicchecking.SymbolicCheckingFormulaItem;

import de.prob2.ui.vomanager.Requirement;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyListProperty;
import javafx.beans.property.SetProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleSetProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;

// Match property order that was previously generated by Gson
// (to avoid unnecessary reordering when re-saving existing files).
@JsonPropertyOrder({
	"name",
	"description",
	"location",
	"lastUsedPreferenceName",
	"requirements",
	"ltlFormulas",
	"ltlPatterns",
	"symbolicCheckingFormulas",
	"symbolicAnimationFormulas",
	"simulationItems",
	"testCases",
	"traces",
	"modelcheckingItems",
	"simulation",
	"visBVisualisation",
})
public class Machine implements DescriptionView.Describable {
	public enum CheckingStatus {
		UNKNOWN, SUCCESSFUL, FAILED, NONE
	}

	public static class MachineCheckingStatus {
		private CheckingStatus status;
		private int numberSuccess;
		private int numberTotal;

		public MachineCheckingStatus(CheckingStatus status, int numberSuccess, int numberTotal) {
			this.status = status;
			this.numberSuccess = numberSuccess;
			this.numberTotal = numberTotal;
		}

		public MachineCheckingStatus(CheckingStatus status) {
			this.status = status;
			this.numberSuccess = 0;
			this.numberTotal = 0;
		}

		public CheckingStatus getStatus() {
			return status;
		}

		public int getNumberSuccess() {
			return numberSuccess;
		}

		public int getNumberTotal() {
			return numberTotal;
		}

		public void setStatus(CheckingStatus status) {
			this.status = status;
		}

		public void setNumberSuccess(int numberSuccess) {
			this.numberSuccess = numberSuccess;
		}

		public void setNumberTotal(int numberTotal) {
			this.numberTotal = numberTotal;
		}
	}
	
	@JsonIgnore
	private final ObjectProperty<MachineCheckingStatus> traceReplayStatus = new SimpleObjectProperty<>(this, "traceReplayStatus", new MachineCheckingStatus(CheckingStatus.NONE));
	@JsonIgnore
	private final ObjectProperty<MachineCheckingStatus> ltlStatus = new SimpleObjectProperty<>(this, "ltlStatus", new MachineCheckingStatus(CheckingStatus.NONE));
	@JsonIgnore
	private final ObjectProperty<MachineCheckingStatus> symbolicCheckingStatus = new SimpleObjectProperty<>(this, "symbolicCheckingStatus", new MachineCheckingStatus(CheckingStatus.NONE));
	@JsonIgnore
	private final ObjectProperty<MachineCheckingStatus> modelcheckingStatus = new SimpleObjectProperty<>(this, "modelcheckingStatus", new MachineCheckingStatus(CheckingStatus.NONE));
	private final StringProperty name;
	private final StringProperty description;
	private final Path location;
	private final StringProperty lastUsedPreferenceName;
	private final ListProperty<Requirement> requirements;
	private final ListProperty<LTLFormulaItem> ltlFormulas;
	private final ListProperty<LTLPatternItem> ltlPatterns;
	private final ListProperty<SymbolicCheckingFormulaItem> symbolicCheckingFormulas;
	private final ListProperty<SymbolicAnimationItem> symbolicAnimationFormulas;
	private final ListProperty<SimulationItem> simulationItems;
	private final ListProperty<TestCaseGenerationItem> testCases;
	private final SetProperty<Path> traces;
	private final ListProperty<ModelCheckingItem> modelcheckingItems;
	private ObjectProperty<Path> simulation;
	private ObjectProperty<Path> visBVisualisation;
	@JsonIgnore
	private PatternManager patternManager = new PatternManager();
	@JsonIgnore
	private final BooleanProperty changed = new SimpleBooleanProperty(false);

	// When deserializing from JSON,
	// all fields that are not listed as constructor parameters
	// are instead filled in using setters after the Machine object is constructed.
	@JsonCreator
	public Machine(
		@JsonProperty("name") final String name,
		@JsonProperty("description") final String description,
		@JsonProperty("location") final Path location
	) {
		this.name = new SimpleStringProperty(this, "name", name);
		this.description = new SimpleStringProperty(this, "description", description);
		this.location = location;
		this.lastUsedPreferenceName = new SimpleStringProperty(this, "lastUsedPreferenceName", Preference.DEFAULT.getName());
		this.requirements = new SimpleListProperty<>(this, "requirements", FXCollections.observableArrayList());
		this.ltlFormulas = new SimpleListProperty<>(this, "ltlFormulas", FXCollections.observableArrayList());
		this.ltlPatterns = new SimpleListProperty<>(this, "ltlPatterns", FXCollections.observableArrayList());
		this.symbolicCheckingFormulas = new SimpleListProperty<>(this, "symbolicCheckingFormulas", FXCollections.observableArrayList());
		this.symbolicAnimationFormulas = new SimpleListProperty<>(this, "symbolicAnimationFormulas", FXCollections.observableArrayList());
		this.simulationItems = new SimpleListProperty<>(this, "simulationItems", FXCollections.observableArrayList());
		this.testCases = new SimpleListProperty<>(this, "testCases", FXCollections.observableArrayList());
		this.traces = new SimpleSetProperty<>(this, "traces", FXCollections.observableSet());
		this.modelcheckingItems = new SimpleListProperty<>(this, "modelcheckingItems", FXCollections.observableArrayList());
		this.simulation = new SimpleObjectProperty<>(this, "simulation", null);
		this.visBVisualisation = new SimpleObjectProperty<>(this, "visBVisualisation", null);
		this.initListeners();
	}
	
	private static Machine.CheckingStatus combineCheckingStatus(final List<? extends IExecutableItem> items) {
		boolean anyEnabled = false;
		boolean anyUnknown = false;
		for(IExecutableItem item : items) {
			if(!item.selected()) {
				continue;
			}
			anyEnabled = true;
			if(item.getChecked() == Checked.FAIL) {
				return Machine.CheckingStatus.FAILED;
			} else if (item.getChecked() == Checked.NOT_CHECKED) {
				anyUnknown = true;
			}
		}
		return anyEnabled ? (anyUnknown? CheckingStatus.UNKNOWN :  Machine.CheckingStatus.SUCCESSFUL) : Machine.CheckingStatus.NONE;
	}

	private static Machine.MachineCheckingStatus combineMachineCheckingStatus(final List<? extends IExecutableItem> items) {
		CheckingStatus status = combineCheckingStatus(items);
		int numberSuccess = (int) items.stream()
				.filter(item -> item.getChecked() == Checked.SUCCESS && item.selected())
				.count();
		int numberTotal = (int) items.stream()
				.filter(IExecutableItem::selected)
				.count();
		return new MachineCheckingStatus(status, numberSuccess, numberTotal);
	}
	
	public static void addCheckingStatusListener(final ReadOnlyListProperty<? extends IExecutableItem> items, final ObjectProperty<Machine.MachineCheckingStatus> statusProperty) {
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
	
	private void initListeners() {
		final InvalidationListener changedListener = o -> this.setChanged(true);
		this.nameProperty().addListener(changedListener);
		this.descriptionProperty().addListener(changedListener);
		this.lastUsedPreferenceNameProperty().addListener(changedListener);
		this.requirementsProperty().addListener(changedListener);
		this.ltlFormulasProperty().addListener(changedListener);
		this.ltlPatternsProperty().addListener(changedListener);
		this.symbolicCheckingFormulasProperty().addListener(changedListener);
		this.symbolicAnimationFormulasProperty().addListener(changedListener);
		this.simulationItemsProperty().addListener(changedListener);
		this.testCasesProperty().addListener(changedListener);
		this.tracesProperty().addListener(changedListener);
		this.modelcheckingItemsProperty().addListener(changedListener);
		this.simulationProperty().addListener(changedListener);
		this.visBVisualizationProperty().addListener(changedListener);

		addCheckingStatusListener(this.ltlFormulasProperty(), this.ltlStatusProperty());
		addCheckingStatusListener(this.symbolicCheckingFormulasProperty(), this.symbolicCheckingStatusProperty());
		addCheckingStatusListener(this.modelcheckingItemsProperty(), this.modelcheckingStatusProperty());
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
	
	public void resetStatus() {
		requirements.forEach(Requirement::reset);
		ltlFormulas.forEach(LTLFormulaItem::reset);
		ltlPatterns.forEach(LTLPatternItem::reset);
		patternManager = new PatternManager();
		symbolicCheckingFormulas.forEach(SymbolicCheckingFormulaItem::reset);
		symbolicAnimationFormulas.forEach(SymbolicAnimationItem::reset);
		simulationItems.forEach(SimulationItem::reset);
		testCases.forEach(TestCaseGenerationItem::reset);
		modelcheckingItems.forEach(ModelCheckingItem::reset);
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

	public ObjectProperty<MachineCheckingStatus> ltlStatusProperty() {
		return this.ltlStatus;
	}
	
	public MachineCheckingStatus getLtlStatus() {
		return this.ltlStatusProperty().get();
	}
	
	public void setLtlStatus(final MachineCheckingStatus status) {
		this.ltlStatusProperty().set(status);
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

	public ListProperty<Requirement> requirementsProperty() {
		return requirements;
	}

	@JsonProperty("requirements")
	public List<Requirement> getRequirements() {
		return requirements.get();
	}

	@JsonProperty("requirements")
	public void setRequirements(final List<Requirement> requirements) {
		this.requirements.setAll(requirements);
	}

	public ListProperty<LTLFormulaItem> ltlFormulasProperty() {
		return ltlFormulas;
	}
	
	@JsonProperty("ltlFormulas")
	public List<LTLFormulaItem> getLTLFormulas() {
		return ltlFormulasProperty().get();
	}
	
	@JsonProperty("ltlFormulas")
	private void setLTLFormulas(final List<LTLFormulaItem> ltlFormulas) {
		this.ltlFormulasProperty().setAll(ltlFormulas);
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

	public ListProperty<SimulationItem> simulationItemsProperty() {
		return simulationItems;
	}

	@JsonProperty("simulationItems")
	public List<SimulationItem> getSimulations() {
		return simulationItems.get();
	}

	@JsonProperty
	private void setSimulationItems(final List<SimulationItem> simulationItems) {
		this.simulationItemsProperty().setAll(simulationItems);
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
	
	@JsonProperty("traces")
	public ObservableSet<Path> getTraceFiles() {
		return this.traces;
	}
	
	public void addTraceFile(Path traceFile) {
		//Note, if the traceFile does already exist and has to be updated,
		//we must remove the traceFile first in order to trigger the SetChangeListener!
		this.traces.remove(traceFile);
		this.traces.add(traceFile);
	}
	
	public void removeTraceFile(Path traceFile) {
		this.traces.remove(traceFile);
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
	
	public SetProperty<Path> tracesProperty() {
		return traces;
	}
	
	// traces is actually a Set, but we ask Jackson to give us a List here,
	// so that the order of the trace files is retained.
	@JsonProperty
	private void setTraces(final List<Path> traces) {
		this.tracesProperty().clear();
		this.tracesProperty().addAll(traces);
	}
	
	public Path getLocation() {
		return this.location;
	}

	public ObjectProperty<Path> simulationProperty() {
		return simulation;
	}

	public Path getSimulation() {
		return simulation.get();
	}

	public void setSimulation(Path simulation) {
		this.simulationProperty().set(simulation);
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

	@Override
	public boolean equals(Object other) {
		if (other == this) {
			return true;
		}
		if (!(other instanceof Machine)) {
			return false;
		}
		Machine otherMachine = (Machine) other;
		return this.getLocation().equals(otherMachine.getLocation());
	}
	
	@Override
	public String toString() {
		return this.getName();
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.getLocation());
	}
	
	public PatternManager getPatternManager() {
		return patternManager;
	}
	
	public void clearPatternManager() {
		patternManager.getPatterns().clear();
	}
}
