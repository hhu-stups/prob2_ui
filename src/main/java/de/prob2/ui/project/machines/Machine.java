package de.prob2.ui.project.machines;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import de.prob.ltl.parser.pattern.PatternManager;
import de.prob.scripting.Api;
import de.prob.scripting.ModelTranslationError;
import de.prob.statespace.StateSpace;

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
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleSetProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;

public class Machine {
	@FunctionalInterface
	public interface Loader extends Serializable {
		StateSpace load(final Api api, final String file, final Map<String, String> prefs) throws IOException, ModelTranslationError;
	}

	public enum Type {
		B(Api::b_load, new String[] {"*.mch", "*.ref", "*.imp"}),
		EVENTB(Api::eventb_load, new String[] {"*.eventb", "*.bum", "*.buc"}),
		CSP(Api::csp_load, new String[] {"*.csp", "*.cspm"}),
		TLA(Api::tla_load, new String[] {"*.tla"}),
		BRULES(Api::brules_load, new String[] {"*.rmch"} ),
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
		
		private Type(final Machine.Loader loader, final String[] extensions) {
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
	}
	
	public enum CheckingStatus {
		UNKNOWN, SUCCESSFUL, FAILED
	}
	
	private transient ObjectProperty<CheckingStatus> ltlStatus;
	private transient ObjectProperty<CheckingStatus> symbolicCheckingStatus;
	private transient ObjectProperty<CheckingStatus> modelcheckingStatus;
	private String name;
	private String description;
	private String location;
	private Machine.Type type;
	private Preference lastUsed;
	private ListProperty<LTLFormulaItem> ltlFormulas;
	private ListProperty<LTLPatternItem> ltlPatterns;
	private ListProperty<SymbolicCheckingFormulaItem> symbolicCheckingFormulas;
	private ObservableSet<File> traces;
	private ListProperty<ModelCheckingItem> modelcheckingItems;
	private transient PatternManager patternManager;
	private transient BooleanProperty changed;

	public Machine(String name, String description, Path location, Machine.Type type) {
		this.name = name;
		this.description = description;
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

	public String getFileName() {
		String pattern = Pattern.quote(System.getProperty("file.separator"));
		String[] splittedFileName = location.split(pattern);
		return splittedFileName[splittedFileName.length - 1];
	}
	
	public Machine.Type getType() {
		return this.type;
	}
	
	public Preference getLastUsed() {
		return lastUsed;
	}
	
	public void setLastUsed(Preference lastUsed) {
		this.lastUsed = lastUsed;
	}
	
	public void resetStatus() {
		if (ltlFormulas != null) {
			ltlFormulas.forEach(LTLFormulaItem::initializeStatus);
		}
		if (ltlPatterns != null) {
			ltlPatterns.forEach(LTLPatternItem::initializeStatus);
		}
		patternManager = new PatternManager();
		if (symbolicCheckingFormulas != null) {
			symbolicCheckingFormulas.forEach(SymbolicCheckingFormulaItem::initializeStatus);
			symbolicCheckingFormulas.forEach(SymbolicCheckingFormulaItem::initializeCounterExamples);
		}
		if (modelcheckingItems != null) {
			modelcheckingItems.forEach(ModelCheckingItem::initializeStatus);
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
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
		this.changed.set(true);
	}

	public String getDescription() {
		return description;
	}
	
	public void setDescription(String description) {
		this.description = description;
		this.changed.set(true);
	}
	
	public ListProperty<LTLFormulaItem> ltlFormulasProperty() {
		return ltlFormulas;
	}
	
	public List<LTLFormulaItem> getLTLFormulas() {
		return ltlFormulasProperty().get();
	}
	
	public void addLTLFormula(LTLFormulaItem formula) {
		ltlFormulas.add(formula);
		this.changed.set(true);
	}
	
	public void removeLTLFormula(LTLFormulaItem formula) {
		ltlFormulas.remove(formula);
		this.changed.set(true);
	}
	
	public ListProperty<LTLPatternItem> ltlPatternsProperty() {
		return ltlPatterns;
	}
	
	public List<LTLPatternItem> getLTLPatterns() {
		return ltlPatternsProperty().get();
	}
	
	public void addLTLPattern(LTLPatternItem pattern) {
		ltlPatterns.add(pattern);
		this.changed.set(true);
	}
	
	public void removeLTLPattern(LTLPatternItem pattern) {
		ltlPatterns.remove(pattern);
		this.changed.set(true);
	}
	
	public ListProperty<SymbolicCheckingFormulaItem> symbolicCheckingFormulasProperty() {
		return symbolicCheckingFormulas;
	}
	
	public List<SymbolicCheckingFormulaItem> getSymbolicCheckingFormulas() {
		return symbolicCheckingFormulas.get();
	}
	
	public void addSymbolicCheckingFormula(SymbolicCheckingFormulaItem formula) {
		symbolicCheckingFormulas.add(formula);
		this.changed.set(true);
	}
	
	public void removeSymbolicCheckingFormula(SymbolicCheckingFormulaItem formula) {
		symbolicCheckingFormulas.remove(formula);
		this.changed.set(true);
	}
	
	public ObservableSet<File> getTraceFiles() {
		return this.traces;
	}
	
	public void addTraceFile(File traceFile) {
		this.traces.add(traceFile);
		this.changed.set(true);
	}
	
	public void removeTraceFile(File traceFile) {
		this.traces.remove(traceFile);
		this.changed.set(true);
	}

	public ListProperty<ModelCheckingItem> modelcheckingItemsProperty() {
		return modelcheckingItems;
	}
	
	public List<ModelCheckingItem> getModelcheckingItems() {
		return modelcheckingItems.get();
	}
	
	public void addModelcheckingItem(ModelCheckingItem item) {
		modelcheckingItems.add(item);
		Platform.runLater(() -> this.changed.set(true));
	}
	
	public void removeModelcheckingItem(ModelCheckingItem item) {
		modelcheckingItems.remove(item);
		Platform.runLater(() -> this.changed.set(true));
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
		if(traces == null) {
			this.traces = new SimpleSetProperty<>(this, "traces", FXCollections.observableSet());
		}
		if(modelcheckingItems == null) {
			this.modelcheckingItems = new SimpleListProperty<>(this, "modelcheckingItems", FXCollections.observableArrayList());
		}
		if(lastUsed == null){
			lastUsed = Preference.DEFAULT;
		}
		this.changed = new SimpleBooleanProperty(false);
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
		return otherMachine.location.equals(this.location) && otherMachine.name.equals(this.name);
	}
	
	@Override
	public String toString() {
		return this.name;
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
