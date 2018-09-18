package de.prob2.ui.animation.symbolic;

import javax.inject.Inject;

import com.google.inject.Singleton;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.symbolic.SymbolicChoosingStage;

@Singleton
public class SymbolicAnimationChoosingStage extends SymbolicChoosingStage<SymbolicAnimationFormulaItem> {
	
	@Inject
	private SymbolicAnimationChoosingStage(final StageManager stageManager) {
		super();
		stageManager.loadFXML(this, "symbolic_animation_choice.fxml");
	}
	
}
