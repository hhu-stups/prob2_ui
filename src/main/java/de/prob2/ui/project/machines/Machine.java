package de.prob2.ui.project.machines;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import de.prob2.ui.verifications.ltl.LTLCheckableItem;
import de.prob2.ui.verifications.ltl.LTLFormulaItem;
import de.prob2.ui.verifications.ltl.patterns.LTLPatternItem;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;

public class Machine extends LTLCheckableItem {
	private String location;
	private ListProperty<LTLFormulaItem> ltlFormulas;
	private ListProperty<LTLPatternItem> ltlPatterns;

	public Machine(String name, String description, Path location) {
		super(name,description);
		this.location = location.toString();
		this.ltlFormulas = new SimpleListProperty<>(this, "ltlFormulas", FXCollections.observableArrayList());
		this.ltlPatterns = new SimpleListProperty<>(this, "ltlPatterns", FXCollections.observableArrayList());
}
	
	public Machine(String name, String description, Path location, ListProperty<LTLFormulaItem> ltlFormulas, 
					ListProperty<LTLPatternItem> ltlPatterns) {
		super(name,description);
		this.location = location.toString();
		this.ltlFormulas = ltlFormulas;
		this.ltlPatterns = ltlPatterns;
	}

	public String getFileName() {
		String pattern = Pattern.quote(System.getProperty("file.separator"));
		String[] splittedFileName = location.split(pattern);
		return splittedFileName[splittedFileName.length - 1];
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
}
