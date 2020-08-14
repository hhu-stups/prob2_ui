package de.prob2.ui.project.machines;

import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

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

public class Machine implements DescriptionView.Describable {
	public enum CheckingStatus {
		UNKNOWN, SUCCESSFUL, FAILED, NONE
	}
	
	public static final JsonDeserializer<Machine> JSON_DESERIALIZER = Machine::new;
	
	private final transient ObjectProperty<CheckingStatus> ltlStatus = new SimpleObjectProperty<>(this, "ltlStatus", CheckingStatus.UNKNOWN);
	private final transient ObjectProperty<CheckingStatus> symbolicCheckingStatus = new SimpleObjectProperty<>(this, "symbolicCheckingStatus", CheckingStatus.UNKNOWN);
	private final transient ObjectProperty<CheckingStatus> modelcheckingStatus = new SimpleObjectProperty<>(this, "modelcheckingStatus", CheckingStatus.UNKNOWN);
	private final StringProperty name;
	private final StringProperty description;
	private final Path location;
	private final StringProperty lastUsedPreferenceName;
	private final ListProperty<LTLFormulaItem> ltlFormulas;
	private final ListProperty<LTLPatternItem> ltlPatterns;
	private final ListProperty<SymbolicCheckingFormulaItem> symbolicCheckingFormulas;
	private final ListProperty<SymbolicAnimationItem> symbolicAnimationFormulas;
	private final ListProperty<TestCaseGenerationItem> testCases;
	private final SetProperty<Path> traces;
	private final ListProperty<ModelCheckingItem> modelcheckingItems;
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
		this.testCases = new SimpleListProperty<>(this, "testCases", FXCollections.observableArrayList());
		this.traces = new SimpleSetProperty<>(this, "traces", FXCollections.observableSet());
		this.modelcheckingItems = new SimpleListProperty<>(this, "modelcheckingItems", FXCollections.observableArrayList());
		
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
		this.testCases = JsonManager.checkDeserialize(context, object, "testCases", new TypeToken<ListProperty<TestCaseGenerationItem>>() {}.getType());
		this.traces = JsonManager.checkDeserialize(context, object, "traces", new TypeToken<SetProperty<Path>>() {}.getType());
		this.modelcheckingItems = JsonManager.checkDeserialize(context, object, "modelcheckingItems", new TypeToken<ListProperty<ModelCheckingItem>>() {}.getType());
		
		this.initListeners();
	}
	
	private static Machine.CheckingStatus combineCheckingStatus(final List<? extends IExecutableItem> items) {
		boolean anyEnabled = false;
		for(IExecutableItem item : items) {
			if(!item.selected()) {
				continue;
			}
			anyEnabled = true;
			if(item.getChecked() == Checked.FAIL) {
				return Machine.CheckingStatus.FAILED;
			} else if (item.getChecked() != Checked.SUCCESS) {
				return Machine.CheckingStatus.UNKNOWN;
			}
		}
		return anyEnabled ? Machine.CheckingStatus.SUCCESSFUL : Machine.CheckingStatus.NONE;
	}
	
	private static void addCheckingStatusListener(final ReadOnlyListProperty<? extends IExecutableItem> items, final ObjectProperty<Machine.CheckingStatus> statusProperty) {
		final InvalidationListener updateListener = o -> Platform.runLater(() -> statusProperty.set(combineCheckingStatus(items)));
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
		this.nameProperty().addListener(o -> this.setChanged(true));
		this.descriptionProperty().addListener(o -> this.setChanged(true));
		
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
		testCases.forEach(TestCaseGenerationItem::reset);
		modelcheckingItems.forEach(ModelCheckingItem::reset);
	}
	
	public ObjectProperty<CheckingStatus> ltlStatusProperty() {
		return this.ltlStatus;
	}
	
	public CheckingStatus getLtlStatus() {
		return this.ltlStatusProperty().get();
	}
	
	public void setLtlStatus(final CheckingStatus status) {
		this.ltlStatusProperty().set(status);
	}

	public ObjectProperty<CheckingStatus> symbolicCheckingStatusProperty() {
		return this.symbolicCheckingStatus;
	}

	public CheckingStatus getSymbolicCheckingStatus() {
		return this.symbolicCheckingStatusProperty().get();
	}

	public void setSymbolicCheckingStatus(final CheckingStatus status) {
		this.symbolicCheckingStatusProperty().set(status);
	}
	
	public ObjectProperty<CheckingStatus> modelcheckingStatusProperty() {
		return this.modelcheckingStatus;
	}
	
	public CheckingStatus getModelcheckingStatus() {
		return this.modelcheckingStatusProperty().get();
	}
	
	public void setModelcheckingStatus(final CheckingStatus status) {
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
	
	public void addLTLFormula(LTLFormulaItem formula) {
		ltlFormulas.add(formula);
		this.setChanged(true);
	}
	
	public void removeLTLFormula(LTLFormulaItem formula) {
		ltlFormulas.remove(formula);
		this.setChanged(true);
	}
	
	public ListProperty<LTLPatternItem> ltlPatternsProperty() {
		return ltlPatterns;
	}
	
	public List<LTLPatternItem> getLTLPatterns() {
		return ltlPatternsProperty().get();
	}
	
	public void addLTLPattern(LTLPatternItem pattern) {
		ltlPatterns.add(pattern);
		this.setChanged(true);
	}
	
	public void removeLTLPattern(LTLPatternItem pattern) {
		ltlPatterns.remove(pattern);
		this.setChanged(true);
	}
	
	public ListProperty<SymbolicCheckingFormulaItem> symbolicCheckingFormulasProperty() {
		return symbolicCheckingFormulas;
	}
	
	public List<SymbolicCheckingFormulaItem> getSymbolicCheckingFormulas() {
		return symbolicCheckingFormulas.get();
	}
	
	public void addSymbolicCheckingFormula(SymbolicCheckingFormulaItem formula) {
		symbolicCheckingFormulas.add(formula);
		this.setChanged(true);
	}
	
	public void removeSymbolicCheckingFormula(SymbolicCheckingFormulaItem formula) {
		symbolicCheckingFormulas.remove(formula);
		this.setChanged(true);
	}
	
	public ListProperty<SymbolicAnimationItem> symbolicAnimationFormulasProperty() {
		return symbolicAnimationFormulas;
	}
	
	public List<SymbolicAnimationItem> getSymbolicAnimationFormulas() {
		return symbolicAnimationFormulas.get();
	}
	
	public void addSymbolicAnimationFormula(SymbolicAnimationItem formula) {
		symbolicAnimationFormulas.add(formula);
		this.setChanged(true);
	}
	
	public void removeSymbolicAnimationFormula(SymbolicAnimationItem formula) {
		symbolicAnimationFormulas.remove(formula);
		this.setChanged(true);
	}
	
	public ListProperty<TestCaseGenerationItem> testCasesProperty() {
		return testCases;
	}
	
	public List<TestCaseGenerationItem> getTestCases() {
		return testCases.get();
	}
	
	public void addTestCase(TestCaseGenerationItem item) {
		testCases.add(item);
		this.setChanged(true);
	}
	
	public void removeTestCase(TestCaseGenerationItem item) {
		testCases.remove(item);
		this.setChanged(true);
	}
	
	public ObservableSet<Path> getTraceFiles() {
		return this.traces;
	}
	
	public void addTraceFile(Path traceFile) {
		//Note, if the traceFile does already exist and has to be updated,
		//we must remove the traceFile first in order to trigger the SetChangeListener!
		this.traces.remove(traceFile);
		this.traces.add(traceFile);
		this.setChanged(true);
	}
	
	public void removeTraceFile(Path traceFile) {
		this.traces.remove(traceFile);
		this.setChanged(true);
	}

	public ListProperty<ModelCheckingItem> modelcheckingItemsProperty() {
		return modelcheckingItems;
	}
	
	public List<ModelCheckingItem> getModelcheckingItems() {
		return modelcheckingItems.get();
	}
	
	public void addModelcheckingItem(ModelCheckingItem item) {
		modelcheckingItems.add(item);
		Platform.runLater(() -> this.setChanged(true));
	}
	
	public void removeModelcheckingItem(ModelCheckingItem item) {
		modelcheckingItems.remove(item);
		Platform.runLater(() -> this.setChanged(true));
	}
	
	public SetProperty<Path> tracesProperty() {
		return traces;
	}
	
	public Path getLocation() {
		return this.location;
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
