package de.prob2.ui.project.machines;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import com.google.common.io.Files;

import de.prob.ltl.parser.pattern.PatternManager;
import de.prob.scripting.FactoryProvider;
import de.prob.scripting.ModelFactory;
import de.prob2.ui.animation.symbolic.SymbolicAnimationItem;
import de.prob2.ui.animation.symbolic.testcasegeneration.TestCaseGenerationItem;
import de.prob2.ui.internal.OnlyDeserialize;
import de.prob2.ui.project.preferences.Preference;
import de.prob2.ui.sharedviews.DescriptionView;
import de.prob2.ui.verifications.ltl.formula.LTLFormulaItem;
import de.prob2.ui.verifications.ltl.patterns.LTLPatternItem;
import de.prob2.ui.verifications.modelchecking.ModelCheckingItem;
import de.prob2.ui.verifications.symbolicchecking.SymbolicCheckingFormulaItem;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SetProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleSetProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;

public class Machine implements DescriptionView.Describable {
	public enum CheckingStatus {
		UNKNOWN, SUCCESSFUL, FAILED
	}
	
	private transient ObjectProperty<CheckingStatus> ltlStatus;
	private transient ObjectProperty<CheckingStatus> symbolicCheckingStatus;
	private transient ObjectProperty<CheckingStatus> symbolicAnimationStatus;
	private transient ObjectProperty<CheckingStatus> modelcheckingStatus;
	private StringProperty name;
	private StringProperty description;
	private Path location;
	/**
	 * No longer used, except for backwards compatibility with old projects. Replaced by {@link #lastUsedPreferenceName}.
	 */
	@OnlyDeserialize
	private ObjectProperty<Preference> lastUsed;
	private StringProperty lastUsedPreferenceName;
	private ListProperty<LTLFormulaItem> ltlFormulas;
	private ListProperty<LTLPatternItem> ltlPatterns;
	private ListProperty<SymbolicCheckingFormulaItem> symbolicCheckingFormulas;
	private ListProperty<SymbolicAnimationItem> symbolicAnimationFormulas;
	private ListProperty<TestCaseGenerationItem> testCases;
	private SetProperty<Path> traces;
	private ListProperty<ModelCheckingItem> modelcheckingItems;
	private transient PatternManager patternManager;
	private transient BooleanProperty changed;

	public Machine(String name, String description, Path location) {
		this.name = new SimpleStringProperty(this, "name", name);
		this.description = new SimpleStringProperty(this, "description", description);
		this.location = location;
		this.replaceMissingWithDefaults();
		this.resetStatus();
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
		if (ltlFormulas != null) {
			ltlFormulas.forEach(LTLFormulaItem::initialize);
		}
		if (ltlPatterns != null) {
			ltlPatterns.forEach(LTLPatternItem::initialize);
		}
		patternManager = new PatternManager();
		if (symbolicCheckingFormulas != null) {
			symbolicCheckingFormulas.forEach(SymbolicCheckingFormulaItem::initialize);
			symbolicCheckingFormulas.forEach(SymbolicCheckingFormulaItem::initializeCounterExamples);
		}
		if (symbolicAnimationFormulas != null) {
			symbolicAnimationFormulas.forEach(SymbolicAnimationItem::initialize);
		}
		if (testCases != null) {
			testCases.forEach(TestCaseGenerationItem::initialize);
		}
		if (modelcheckingItems != null) {
			modelcheckingItems.forEach(ModelCheckingItem::initialize);
		}
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

	public ObjectProperty<CheckingStatus> symbolicAnimationStatusProperty() {
		return this.symbolicAnimationStatus;
	}

	public CheckingStatus getSymbolicAnimationStatus() {
		return this.symbolicAnimationStatusProperty().get();
	}

	public void setSymbolicAnimationStatus(final CheckingStatus status) {
		this.symbolicAnimationStatusProperty().set(status);
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
		
	public void replaceMissingWithDefaults() {
		if (ltlStatus == null) {
			this.ltlStatus = new SimpleObjectProperty<>(this, "ltlStatus", CheckingStatus.UNKNOWN);
		}
		if (symbolicCheckingStatus == null) {
			this.symbolicCheckingStatus = new SimpleObjectProperty<>(this, "symbolicCheckingStatus", CheckingStatus.UNKNOWN);
		}
		if (symbolicAnimationStatus == null) {
			this.symbolicAnimationStatus = new SimpleObjectProperty<>(this, "symbolicAnimationStatus", CheckingStatus.UNKNOWN);
		}
		if (modelcheckingStatus == null) {
			this.modelcheckingStatus = new SimpleObjectProperty<>(this, "modelcheckingStatus", CheckingStatus.UNKNOWN);
		}
		if(ltlFormulas == null) {
			this.ltlFormulas = new SimpleListProperty<>(this, "ltlFormulas", FXCollections.observableArrayList());
		}
		if(ltlPatterns == null) {
			this.ltlPatterns = new SimpleListProperty<>(this, "ltlPatterns", FXCollections.observableArrayList());
		}
		if(symbolicCheckingFormulas == null) {
			this.symbolicCheckingFormulas = new SimpleListProperty<>(this, "symbolicCheckingFormulas", FXCollections.observableArrayList());
		}
		if(symbolicAnimationFormulas == null) {
			this.symbolicAnimationFormulas = new SimpleListProperty<>(this, "symbolicAnimationFormulas", FXCollections.observableArrayList());
		}
		if(testCases == null) {
			this.testCases = new SimpleListProperty<>(this, "testCases", FXCollections.observableArrayList());
		}
		if(traces == null) {
			this.traces = new SimpleSetProperty<>(this, "traces", FXCollections.observableSet());
		}
		if(modelcheckingItems == null) {
			this.modelcheckingItems = new SimpleListProperty<>(this, "modelcheckingItems", FXCollections.observableArrayList());
		}
		if (lastUsedPreferenceName == null) {
			final Preference pref = this.lastUsed == null || this.lastUsed.get() == null ? Preference.DEFAULT : this.lastUsed.get();
			lastUsedPreferenceName = new SimpleStringProperty(this, "lastUsedPreferenceName", pref.getName());
		}
		this.changed = new SimpleBooleanProperty(false);
		this.nameProperty().addListener(o -> this.setChanged(true));
		this.descriptionProperty().addListener(o -> this.setChanged(true));
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
