package de.prob2.ui.animation.symbolic;

import java.util.ResourceBundle;

import javax.inject.Inject;

import com.google.inject.Singleton;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.symbolic.SymbolicChoosingStage;

@Singleton
public class SymbolicAnimationChoosingStage extends SymbolicChoosingStage<SymbolicAnimationItem> {
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
	protected SymbolicAnimationItem extractItem() {
		return new SymbolicAnimationItem(this.extractFormula(), this.getExecutionType());
	}
	
	@Override
	protected boolean updateFormula(SymbolicAnimationItem item) {
		Machine currentMachine = currentProject.getCurrentMachine();
		
		SymbolicAnimationItem newItem = this.extractItem();
		if(currentMachine.getSymbolicAnimationFormulas().stream().noneMatch(newItem::settingsEqual)) {
			currentMachine.getSymbolicAnimationFormulas().set(currentMachine.getSymbolicAnimationFormulas().indexOf(item), newItem);
			return true;
		}
		return false;
	}
}
