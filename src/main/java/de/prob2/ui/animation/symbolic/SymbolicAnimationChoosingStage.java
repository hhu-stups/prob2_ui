package de.prob2.ui.animation.symbolic;

import java.util.ResourceBundle;

import javax.inject.Inject;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.symbolic.SymbolicChoosingStage;
import de.prob2.ui.symbolic.SymbolicGUIType;

public class SymbolicAnimationChoosingStage extends SymbolicChoosingStage<SymbolicAnimationItem, SymbolicAnimationType> {
	@Inject
	private SymbolicAnimationChoosingStage(
		final StageManager stageManager,
		final SymbolicAnimationItemHandler symbolicAnimationItemHandler,
		final ResourceBundle bundle,
		final CurrentProject currentProject,
		final CurrentTrace currentTrace
	) {
		super(bundle, currentProject, currentTrace, symbolicAnimationItemHandler);
		stageManager.loadFXML(this, "symbolic_animation_choice.fxml");
	}
	
	@Override
	public SymbolicGUIType getGUIType(final SymbolicAnimationType item) {
		switch (item) {
			case SEQUENCE:
				return SymbolicGUIType.TEXT_FIELD;
			
			case FIND_VALID_STATE:
				return SymbolicGUIType.PREDICATE;
			
			default:
				throw new AssertionError();
		}
	}
	
	@Override
	protected SymbolicAnimationItem extractItem() {
		return new SymbolicAnimationItem(this.extractFormula(), this.getExecutionType());
	}
}
