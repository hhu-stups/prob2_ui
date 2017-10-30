package de.prob2.ui.verifications.symbolicchecking;

import javafx.beans.NamedArg;

public class SymbolicCheckingItem {
	
	public enum GUIType {
		NONE, TEXT_FIELD, CHOICE_BOX
	}
	
	private SymbolicCheckingType checkingType;
	
	private GUIType guiType;
	
	public SymbolicCheckingItem(@NamedArg("checkingType") SymbolicCheckingType name, @NamedArg("guiType") GUIType guiType) {
		this.checkingType = name;
		this.guiType = guiType;
	}
	
	public String toString() {
		return checkingType.name();
	}
	
	public SymbolicCheckingType getCheckingType() {
		return checkingType;
	}
	
	public GUIType getGUIType() {
		return guiType;
	}

}
