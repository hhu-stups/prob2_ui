package de.prob2.ui.dotty;

import javafx.beans.NamedArg;

public class DotChoiceItem {
	
	private DotVisualisationType visualisationType;
	
	public DotChoiceItem(@NamedArg("visualisationType") DotVisualisationType name) {
		this.visualisationType = name;
	}
	
	public String toString() {
		return visualisationType.name();
	}
	
	public DotVisualisationType geVisualisationType() {
		return visualisationType;
	}
	
	
}
