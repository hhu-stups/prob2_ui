package de.prob2.ui.project.machines;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import de.prob.ltl.parser.pattern.PatternManager;
import de.prob.scripting.Api;
import de.prob.scripting.ModelTranslationError;
import de.prob.statespace.StateSpace;
import de.prob2.ui.animation.symbolic.SymbolicAnimationFormulaItem;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.project.preferences.Preference;
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

public class Machine {
	@FunctionalInterface
	public interface Loader extends Serializable {
		StateSpace load(final Api api, final String file, final Map<String, String> prefs) throws IOException, ModelTranslationError;
	}

	public enum Type {
		B(Api::b_load, new String[] {"*.mch", "*.ref", "*.imp", "*.sys"}),
		EVENTB(Api::eventb_load, new String[] {"*.eventb", "*.bum", "*.buc"}),
		CSP(Api::csp_load, new String[] {"*.csp", "*.cspm"}),
		TLA(Api::tla_load, new String[] {"*.tla"}),
		BRULES(Api::brules_load, new String[] {"*.rmch"}),
		XTL(Api::xtl_load, new String[] {"*.P", "*.pl"}),
		ALLOY(Api::alloy_load, new String[] {"*.als"}),
		;
		
		private static final Map<String, Machine.Type> extensionToTypeMap;
		static {
			extensionToTypeMap = new HashMap<>();
			for (final Machine.Type type : Machine.Type.values()) {
				for (final String ext : type.getExtensions()) {
					extensionToTypeMap.put(ext, type);
				}
			}
		}
		
		private final Machine.Loader loader;
		private final String[] extensions;
		
		Type(final Machine.Loader loader, final String[] extensions) {
			this.loader = loader;
			this.extensions = extensions;
		}
		
		public static Map<String, Machine.Type> getExtensionToTypeMap() {
			return Collections.unmodifiableMap(extensionToTypeMap);
		}
		
		public static Machine.Type fromExtension(final String ext) {
			final Machine.Type type = getExtensionToTypeMap().get("*." + ext);
			if (type == null) {
				throw new IllegalArgumentException(String.format("Could not determine machine type for extension %s", ext));
			}
			return type;
		}
		
		public Machine.Loader getLoader() {
			return this.loader;
		}
		
		public String[] getExtensions() {
			return this.extensions.clone();
		}
		
		public String getExtensionsAsString() {
			return String.join(", ", this.extensions);
		}
	}
	
	public enum CheckingStatus {
		UNKNOWN, SUCCESSFUL, FAILED
	}
	
	private transient ObjectProperty<CheckingStatus> ltlStatus;
	private transient ObjectProperty<CheckingStatus> symbolicCheckingStatus;
	private transient ObjectProperty<CheckingStatus> modelcheckingStatus;
	private StringProperty name;
	private StringProperty description;
	private String location;
	private Machine.Type type;
	private ObjectProperty<Preference> lastUsed;
	private ListProperty<LTLFormulaItem> ltlFormulas;
	private ListProperty<LTLPatternItem> ltlPatterns;
	private ListProperty<SymbolicCheckingFormulaItem> symbolicCheckingFormulas;
	private ListProperty<SymbolicAnimationFormulaItem> symbolicAnimationFormulas;
	private SetProperty<Path> traces;
	private ListProperty<ModelCheckingItem> modelcheckingItems;
	private transient PatternManager patternManager;
	private transient BooleanProperty changed;

	public Machine(String name, String description, Path location, Machine.Type type) {
		this.name = new SimpleStringProperty(this, "name", name);
		this.description = new SimpleStringProperty(this, "description", description);
		this.location = location.toString();
		this.type = type;
		this.replaceMissingWithDefaults();
		this.resetStatus();
	}
	
	public Machine(String name, String description, Path location) {
		this(name, description, location,
			Machine.Type.fromExtension(StageManager.getExtension(location.getFileName().toString())));
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
	
	public Machine.Type getType() {
		return this.type;
	}
	
	public ObjectProperty<Preference> lastUsedProperty() {
		return this.lastUsed;
	}
	
	public Preference getLastUsed() {
		return this.lastUsedProperty().get();
	}
	
	public void setLastUsed(final Preference lastUsed) {
		this.lastUsedProperty().set(lastUsed);
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
			symbolicAnimationFormulas.forEach(SymbolicAnimationFormulaItem::initialize);
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
	
	public ListProperty<SymbolicAnimationFormulaItem> symbolicAnimationFormulasProperty() {
		return symbolicAnimationFormulas;
	}
	
	public List<SymbolicAnimationFormulaItem> getSymbolicAnimationFormulas() {
		return symbolicAnimationFormulas.get();
	}
	
	public void addSymbolicAnimationFormula(SymbolicAnimationFormulaItem formula) {
		symbolicAnimationFormulas.add(formula);
		this.setChanged(true);
	}
	
	public void removeSymbolicAnimationFormula(SymbolicAnimationFormulaItem formula) {
		symbolicAnimationFormulas.remove(formula);
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
		if (modelcheckingStatus == null) {
			this.modelcheckingStatus = new SimpleObjectProperty<>(this, "modelcheckingStatus", CheckingStatus.UNKNOWN);
		}
		if (type == null) {
			this.type = Machine.Type.B;
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
		if(traces == null) {
			this.traces = new SimpleSetProperty<>(this, "traces", FXCollections.observableSet());
		}
		if(modelcheckingItems == null) {
			this.modelcheckingItems = new SimpleListProperty<>(this, "modelcheckingItems", FXCollections.observableArrayList());
		}
		if(lastUsed == null){
			lastUsed = new SimpleObjectProperty<>(this, "lastUsed", Preference.DEFAULT);
		}
		this.changed = new SimpleBooleanProperty(false);
		this.nameProperty().addListener(o -> this.setChanged(true));
		this.descriptionProperty().addListener(o -> this.setChanged(true));
	}
	
	public Path getPath() {
		return Paths.get(location);
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
		return otherMachine.location.equals(this.location);
	}
	
	@Override
	public String toString() {
		return this.getName();
	}

	@Override
	public int hashCode() {
		return Objects.hash(location);
	}
	
	public PatternManager getPatternManager() {
		return patternManager;
	}
	
	public void clearPatternManager() {
		patternManager.getPatterns().clear();
	}
}
