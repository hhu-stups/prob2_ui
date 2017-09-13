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
import java.util.regex.Pattern;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;

import de.prob.ltl.parser.pattern.PatternManager;
import de.prob.scripting.Api;
import de.prob.scripting.ModelTranslationError;
import de.prob.statespace.StateSpace;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.verifications.cbc.CBCFormulaItem;
import de.prob2.ui.verifications.ltl.formula.LTLFormulaItem;
import de.prob2.ui.verifications.ltl.patterns.LTLPatternItem;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.scene.paint.Color;

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
	
	protected transient FontAwesomeIconView ltlstatus;
	protected transient FontAwesomeIconView cbcstatus;
	private String name;
	private String description;
	private String location;
	private Machine.Type type;
	private ListProperty<LTLFormulaItem> ltlFormulas;
	private ListProperty<LTLPatternItem> ltlPatterns;
	private ListProperty<CBCFormulaItem> cbcFormulas;
	private transient PatternManager patternManager;
	private transient BooleanProperty changed = new SimpleBooleanProperty(false);

	public Machine(String name, String description, Path location, Machine.Type type) {
		initializeLTLStatus();
		initializeCBCStatus();
		this.name = name;
		this.description = description;
		this.location = location.toString();
		this.type = type;
		this.ltlFormulas = new SimpleListProperty<>(this, "ltlFormulas", FXCollections.observableArrayList());
		this.ltlPatterns = new SimpleListProperty<>(this, "ltlPatterns", FXCollections.observableArrayList());
		this.cbcFormulas = new SimpleListProperty<>(this, "cbcFormulas", FXCollections.observableArrayList());
	}
	
	public Machine(String name, String description, Path location) {
		this(name, description, location,
			Machine.Type.fromExtension(StageManager.getExtension(location.getFileName().toString())));
	}
	
	public void initialize() {
		initializeLTLStatus();
		initializeCBCStatus();
		for(CBCFormulaItem item : cbcFormulas) {
			item.initializeCounterExamples();
		}
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
	
	public void initializeLTLStatus() {
		this.ltlstatus = new FontAwesomeIconView(FontAwesomeIcon.QUESTION_CIRCLE);
		this.ltlstatus.setFill(Color.BLUE);
		if (ltlFormulas != null) {
			for (LTLFormulaItem item : ltlFormulas) {
				item.initializeStatus();
			}
		}
		if (ltlPatterns != null) {
			for (LTLPatternItem item : ltlPatterns) {
				item.initializeStatus();
			}
		}
		patternManager = new PatternManager();
	}
	
	public void initializeCBCStatus() {
		this.cbcstatus = new FontAwesomeIconView(FontAwesomeIcon.QUESTION_CIRCLE);
		this.cbcstatus.setFill(Color.BLUE);
		if (cbcFormulas != null) {
			for (CBCFormulaItem item : cbcFormulas) {
				item.initializeStatus();
			}
		}
	}
	
	public FontAwesomeIconView getLTLStatus() {
		return ltlstatus;
	}
	
	public FontAwesomeIconView getCBCStatus() {
		return cbcstatus;
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
	
	public void setLTLCheckedSuccessful() {
		FontAwesomeIconView icon = new FontAwesomeIconView(FontAwesomeIcon.CHECK);
		icon.setFill(Color.GREEN);
		this.ltlstatus = icon;
	}

	public void setLTLCheckedFailed() {
		FontAwesomeIconView icon = new FontAwesomeIconView(FontAwesomeIcon.REMOVE);
		icon.setFill(Color.RED);
		this.ltlstatus = icon;
	}
	
	public void setCBCCheckedSuccessful() {
		FontAwesomeIconView icon = new FontAwesomeIconView(FontAwesomeIcon.CHECK);
		icon.setFill(Color.GREEN);
		this.cbcstatus = icon;
	}

	public void setCBCCheckedFailed() {
		FontAwesomeIconView icon = new FontAwesomeIconView(FontAwesomeIcon.REMOVE);
		icon.setFill(Color.RED);
		this.cbcstatus = icon;
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
	
	public ListProperty<CBCFormulaItem> cbcFormulasProperty() {
		return cbcFormulas;
	}
	
	public List<CBCFormulaItem> getCBCFormulas() {
		return cbcFormulas.get();
	}
	
	public void addCBCFormula(CBCFormulaItem formula) {
		cbcFormulas.add(formula);
		this.changed.set(true);
	}
	
	public void removeCBCFormula(CBCFormulaItem formula) {
		cbcFormulas.remove(formula);
		this.changed.set(true);
	}
	
		
	public void replaceMissingWithDefaults() {
		if (type == null) {
			this.type = Machine.Type.B;
		}
		if(ltlFormulas == null) {
			this.ltlFormulas = new SimpleListProperty<>(this, "ltlFormulas", FXCollections.observableArrayList());
		}
		if(ltlPatterns == null) {
			this.ltlPatterns = new SimpleListProperty<>(this, "ltlPatterns", FXCollections.observableArrayList());
		}
		if(cbcFormulas == null) {
			this.cbcFormulas = new SimpleListProperty<>(this, "cbcFormulas", FXCollections.observableArrayList());
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
