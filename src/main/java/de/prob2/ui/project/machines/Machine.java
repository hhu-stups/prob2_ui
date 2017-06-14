package de.prob2.ui.project.machines;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import de.prob.scripting.Api;
import de.prob.scripting.ModelTranslationError;
import de.prob.statespace.StateSpace;

import de.prob2.ui.verifications.ltl.LTLCheckableItem;
import de.prob2.ui.verifications.ltl.LTLFormulaItem;
import de.prob2.ui.verifications.ltl.patterns.LTLPatternItem;

import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;

public class Machine extends LTLCheckableItem {
	@FunctionalInterface
	public interface Loader extends Serializable {
		StateSpace load(final Api api, final String file, final Map<String, String> prefs) throws IOException, ModelTranslationError;
	}

	public enum Type {
		B(Api::b_load, new String[] {"*.mch", "*.ref", "*.imp"}),
		EVENTB(Api::eventb_load, new String[] {"*.eventb", "*.bum", "*.buc"}),
		CSP(Api::csp_load, new String[] {"*.cspm"}),
		TLA(Api::tla_load, new String[] {"*.tla"}),
		;
		
		private final Loader loader;
		private final String[] extensions;
		
		private Type(final Loader loader, final String[] extensions) {
			this.loader = loader;
			this.extensions = extensions;
		}
		
		public Loader getLoader() {
			return this.loader;
		}
		
		public String[] getExtensions() {
			return this.extensions.clone();
		}
	}
	
	private String location;
	private Machine.Type type;
	private ListProperty<LTLFormulaItem> ltlFormulas;
	private ListProperty<LTLPatternItem> ltlPatterns;

	public Machine(String name, String description, Path location, Machine.Type type) {
		super(name,description);
		this.location = location.toString();
		this.type = type;
		this.ltlFormulas = new SimpleListProperty<>(this, "ltlFormulas", FXCollections.observableArrayList());
		this.ltlPatterns = new SimpleListProperty<>(this, "ltlPatterns", FXCollections.observableArrayList());
	}

	public String getFileName() {
		String pattern = Pattern.quote(System.getProperty("file.separator"));
		String[] splittedFileName = location.split(pattern);
		return splittedFileName[splittedFileName.length - 1];
	}
	
	public Machine.Type getType() {
		return this.type;
	}
	
	@Override
	public void initializeStatus() {
		super.initializeStatus();
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
	}
		
	public ListProperty<LTLFormulaItem> ltlFormulasProperty() {
		return ltlFormulas;
	}
	
	public List<LTLFormulaItem> getFormulas() {
		return ltlFormulasProperty().get();
	}
	
	public void addLTLFormula(LTLFormulaItem formula) {
		ltlFormulas.add(formula);
	}
	
	public void removeLTLFormula(LTLFormulaItem formula) {
		ltlFormulas.remove(formula);
	}
	
	public ListProperty<LTLPatternItem> ltlPatternsProperty() {
		return ltlPatterns;
	}
	
	public List<LTLPatternItem> getPatterns() {
		return ltlPatternsProperty().get();
	}
	
	public void addLTLPattern(LTLPatternItem pattern) {
		ltlPatterns.add(pattern);
	}
	
	public void removeLTLPattern(LTLPatternItem pattern) {
		ltlPatterns.remove(pattern);
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
		return this.name;
	}

	@Override
	public int hashCode() {
		return Objects.hash(location);
	}
	

}
