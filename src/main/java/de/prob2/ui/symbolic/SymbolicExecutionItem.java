package de.prob2.ui.symbolic;

import javafx.beans.NamedArg;

public class SymbolicExecutionItem {
	
	private SymbolicExecutionType executionType;
	
	private SymbolicGUIType guiType;
	
	public SymbolicExecutionItem(@NamedArg("executionType") SymbolicExecutionType executionType, @NamedArg("guiType") SymbolicGUIType guiType) {
		this.executionType = executionType;
		this.guiType = guiType;
	}
	
	@Override
	public String toString() {
		return executionType.getName();
	}
	
	public SymbolicExecutionType getExecutionType() {
		return executionType;
	}
	
	public SymbolicGUIType getGUIType() {
		return guiType;
	}

}