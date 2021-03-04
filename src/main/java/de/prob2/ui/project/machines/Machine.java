package de.prob2.ui.project.machines;

import com.google.common.io.Files;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import de.prob.json.JsonManager;
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
import javafx.collections.ObservableSet;

import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

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
	
	public static final JsonDeserializer<Machine> JSON_DESERIALIZER = Machine::new;

	private final transient ObjectProperty<MachineCheckingStatus> traceReplayStatus = new SimpleObjectProperty<>(this, "traceReplayStatus", new MachineCheckingStatus(CheckingStatus.NONE));
	private final transient ObjectProperty<MachineCheckingStatus> ltlStatus = new SimpleObjectProperty<>(this, "ltlStatus", new MachineCheckingStatus(CheckingStatus.NONE));
	private final transient ObjectProperty<MachineCheckingStatus> symbolicCheckingStatus = new SimpleObjectProperty<>(this, "symbolicCheckingStatus", new MachineCheckingStatus(CheckingStatus.NONE));
	private final transient ObjectProperty<MachineCheckingStatus> modelcheckingStatus = new SimpleObjectProperty<>(this, "modelcheckingStatus", new MachineCheckingStatus(CheckingStatus.NONE));
	private final StringProperty name;
	private final StringProperty description;
	private final Path location;
	private final StringProperty lastUsedPreferenceName;
	private final ListProperty<LTLFormulaItem> ltlFormulas;
	private final ListProperty<LTLPatternItem> ltlPatterns;
	private final ListProperty<SymbolicCheckingFormulaItem> symbolicCheckingFormulas;
	private final ListProperty<SymbolicAnimationItem> symbolicAnimationFormulas;
	private final ListProperty<SimulationItem> simulationItems;
	private final ListProperty<TestCaseGenerationItem> testCases;
	private final SetProperty<Path> traces;
	private final ListProperty<ModelCheckingItem> modelcheckingItems;
	private ObjectProperty<Path> visBVisualisation;
	private transient PatternManager patternManager = new PatternManager();
	private final transient BooleanProperty changed = new SimpleBooleanProperty(false);

	public Machine(String name, String description, Path location) {
		this.name = new SimpleStringProperty(this, "name", name);
		this.description = new SimpleStringProperty(this, "description", description);
		this.location = location;
		this.lastUsedPreferenceName = new SimpleStringProperty(this, "lastUsedPreferenceName", Preference.DEFAULT.getName());
		this.ltlFormulas = new SimpleListProperty<>(this, "ltlFormulas", FXCollections.observableArrayList());
		this.ltlPatterns = new SimpleListProperty<>(this, "ltlPatterns", FXCollections.observableArrayList());
		this.symbolicCheckingFormulas = new SimpleListProperty<>(this, "symbolicCheckingFormulas", FXCollections.observableArrayList());
		this.symbolicAnimationFormulas = new SimpleListProperty<>(this, "symbolicAnimationFormulas", FXCollections.observableArrayList());
		this.simulationItems = new SimpleListProperty<>(this, "simulationItems", FXCollections.observableArrayList());
		this.testCases = new SimpleListProperty<>(this, "testCases", FXCollections.observableArrayList());
		this.traces = new SimpleSetProperty<>(this, "traces", FXCollections.observableSet());
		this.modelcheckingItems = new SimpleListProperty<>(this, "modelcheckingItems", FXCollections.observableArrayList());
		this.visBVisualisation = new SimpleObjectProperty<>(this, "visBVisualisation", null);
		this.initListeners();
	}
	
	private Machine(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) {
		final JsonObject object = json.getAsJsonObject();
		this.name = JsonManager.checkDeserialize(context, object, "name", StringProperty.class);
		this.description = JsonManager.checkDeserialize(context, object, "description", StringProperty.class);
		this.location = JsonManager.checkDeserialize(context, object, "location", Path.class);
		this.lastUsedPreferenceName = JsonManager.checkDeserialize(context, object, "lastUsedPreferenceName", StringProperty.class);
		this.ltlFormulas = JsonManager.checkDeserialize(context, object, "ltlFormulas", new TypeToken<ListProperty<LTLFormulaItem>>() {}.getType());
		this.ltlPatterns = JsonManager.checkDeserialize(context, object, "ltlPatterns", new TypeToken<ListProperty<LTLPatternItem>>() {}.getType());
		this.symbolicCheckingFormulas = JsonManager.checkDeserialize(context, object, "symbolicCheckingFormulas", new TypeToken<ListProperty<SymbolicCheckingFormulaItem>>() {}.getType());
		this.symbolicAnimationFormulas = JsonManager.checkDeserialize(context, object, "symbolicAnimationFormulas", new TypeToken<ListProperty<SymbolicAnimationItem>>() {}.getType());
		this.simulationItems = JsonManager.checkDeserialize(context, object, "simulationItems", new TypeToken<ListProperty<SimulationItem>>() {}.getType());
		this.testCases = JsonManager.checkDeserialize(context, object, "testCases", new TypeToken<ListProperty<TestCaseGenerationItem>>() {}.getType());
		this.traces = JsonManager.checkDeserialize(context, object, "traces", new TypeToken<SetProperty<Path>>() {}.getType());
		this.modelcheckingItems = JsonManager.checkDeserialize(context, object, "modelcheckingItems", new TypeToken<ListProperty<ModelCheckingItem>>() {}.getType());
		this.visBVisualisation = JsonManager.checkDeserialize(context, object, "visBVisualisation", new TypeToken<ObjectProperty<Path>>() {}.getType());
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
		this.ltlFormulasProperty().addListener(changedListener);
		this.ltlPatternsProperty().addListener(changedListener);
		this.symbolicCheckingFormulasProperty().addListener(changedListener);
		this.symbolicAnimationFormulasProperty().addListener(changedListener);
		this.simulationItemsProperty().addListener(changedListener);
		this.testCasesProperty().addListener(changedListener);
		this.tracesProperty().addListener(changedListener);
		this.modelcheckingItemsProperty().addListener(changedListener);
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
	
	public Class<? extends ModelFactory<?>> getModelFactoryClass() {
		return FactoryProvider.factoryClassFromExtension(
			Files.getFileExtension(this.getLocation().getFileName().toString())
		);
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
	
	public ListProperty<LTLFormulaItem> ltlFormulasProperty() {
		return ltlFormulas;
	}
	
	public List<LTLFormulaItem> getLTLFormulas() {
		return ltlFormulasProperty().get();
	}
	
	public ListProperty<LTLPatternItem> ltlPatternsProperty() {
		return ltlPatterns;
	}
	
	public List<LTLPatternItem> getLTLPatterns() {
		return ltlPatternsProperty().get();
	}
	
	public ListProperty<SymbolicCheckingFormulaItem> symbolicCheckingFormulasProperty() {
		return symbolicCheckingFormulas;
	}
	
	public List<SymbolicCheckingFormulaItem> getSymbolicCheckingFormulas() {
		return symbolicCheckingFormulas.get();
	}
	
	public ListProperty<SymbolicAnimationItem> symbolicAnimationFormulasProperty() {
		return symbolicAnimationFormulas;
	}
	
	public List<SymbolicAnimationItem> getSymbolicAnimationFormulas() {
		return symbolicAnimationFormulas.get();
	}

	public ListProperty<SimulationItem> simulationItemsProperty() {
		return simulationItems;
	}

	public List<SimulationItem> getSimulations() {
		return simulationItems.get();
	}

	public ListProperty<TestCaseGenerationItem> testCasesProperty() {
		return testCases;
	}
	
	public List<TestCaseGenerationItem> getTestCases() {
		return testCases.get();
	}
	
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
	
	public SetProperty<Path> tracesProperty() {
		return traces;
	}
	
	public Path getLocation() {
		return this.location;
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
