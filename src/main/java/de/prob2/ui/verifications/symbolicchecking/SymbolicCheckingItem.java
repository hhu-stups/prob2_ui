package de.prob2.ui.verifications.symbolicchecking;

import javafx.beans.NamedArg;

public class SymbolicCheckingItem {
	
	public enum GUIType {
		
		NONE, TEXT_FIELD, CHOICE_BOX
	}
	
	public enum CheckingType {
	    SEQUENCE,INVARIANTS,DEADLOCK,FIND_DEADLOCK,FIND_VALID_STATE, CHECK_ALL_OPERATIONS, 
	    FIND_REDUNDANT_INVARIANTS, CHECK_REFINEMENT, CHECK_ASSERTIONS, IC3, TINDUCTION, KINDUCTION, BMC
	}
	
	private CheckingType checkingType;
	
	private GUIType guiType;
	
	public SymbolicCheckingItem(@NamedArg("checkingType") CheckingType name, @NamedArg("guiType") GUIType guiType) {
		this.checkingType = name;
		this.guiType = guiType;
	}
	
	public String toString() {
		return checkingType.name();
	}
	
	public CheckingType getCheckingType() {
		return checkingType;
	}
	
	public GUIType getGUIType() {
		return guiType;
	}
	
	public void setCheckingType(CheckingType checkingType) {
		this.checkingType = checkingType;
	}
	
	public void setGUIType(GUIType guiType) {
		this.guiType = guiType;
	}

}
