package de.prob2.ui.verifications.symbolicchecking;

import javafx.beans.NamedArg;

public class SymbolicCheckingItem {
	
	public enum GUIType {
		NONE, TEXT_FIELD, CHOICE_BOX
	}
	
	private SymbolicCheckingType checkingType;
	
	private GUIType guiType;
	
	public SymbolicCheckingItem(@NamedArg("checkingType") SymbolicCheckingType checkingType, @NamedArg("guiType") GUIType guiType) {
		this.checkingType = checkingType;
		this.guiType = guiType;
	}
	
	@Override
	public String toString() {
		return checkingType.getName();
	}
	
	public SymbolicCheckingType getCheckingType() {
		return checkingType;
	}
	
	public GUIType getGUIType() {
		return guiType;
	}

}
