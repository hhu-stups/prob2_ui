package de.prob2.ui.animation.symbolic;

import javafx.beans.NamedArg;

public class SymbolicAnimationItem {
	
	public enum GUIType {
		NONE, TEXT_FIELD, CHOICE_BOX
	}
	
	private SymbolicAnimationType animationType;
	
	private GUIType guiType;
	
	public SymbolicAnimationItem(@NamedArg("animationType") SymbolicAnimationType animationType, @NamedArg("guiType") GUIType guiType) {
		this.animationType = animationType;
		this.guiType = guiType;
	}
	
	@Override
	public String toString() {
		return animationType.getName();
	}
	
	public SymbolicAnimationType getAnimationType() {
		return animationType;
	}
	
	public GUIType getGUIType() {
		return guiType;
	}

}
