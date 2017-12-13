package de.prob2.ui.dotty;

import javafx.beans.NamedArg;

public class DotChoiceItem {
	
	private DotVisualisationType visualisationType;
	
	private boolean hasFormula;
	
	public DotChoiceItem(@NamedArg("visualisationType") DotVisualisationType name, @NamedArg("hasFormula") boolean hasFormula) {
		this.visualisationType = name;
		this.hasFormula = hasFormula;
	}
	
	public String toString() {
		return visualisationType.name();
	}
	
	public DotVisualisationType geVisualisationType() {
		return visualisationType;
	}
	
	public boolean hasFormula() {
		return hasFormula;
	}
	
	
}
