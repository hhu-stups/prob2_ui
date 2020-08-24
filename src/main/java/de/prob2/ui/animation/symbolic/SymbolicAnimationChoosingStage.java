package de.prob2.ui.animation.symbolic;

import java.util.ResourceBundle;

import javax.inject.Inject;

import com.google.inject.Singleton;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.symbolic.SymbolicChoosingStage;
import de.prob2.ui.symbolic.SymbolicExecutionType;

import javafx.fxml.FXML;

@Singleton
public class SymbolicAnimationChoosingStage extends SymbolicChoosingStage<SymbolicAnimationItem> {
	private final SymbolicAnimationItemHandler symbolicAnimationItemHandler;
	
	@Inject
	private SymbolicAnimationChoosingStage(
		final StageManager stageManager,
		final SymbolicAnimationItemHandler symbolicAnimationItemHandler,
		final ResourceBundle bundle,
		final CurrentProject currentProject,
		final CurrentTrace currentTrace
	) {
		super(bundle, currentProject, currentTrace);
		this.symbolicAnimationItemHandler = symbolicAnimationItemHandler;
		stageManager.loadFXML(this, "symbolic_animation_choice.fxml");
	}
	
	@Override
	protected boolean updateFormula(SymbolicAnimationItem item) {
		Machine currentMachine = currentProject.getCurrentMachine();
		String formula = extractFormula();
		
		SymbolicAnimationItem newItem = new SymbolicAnimationItem(formula, this.getExecutionType());
		if(currentMachine.getSymbolicAnimationFormulas().stream().noneMatch(newItem::settingsEqual)) {
			currentMachine.getSymbolicAnimationFormulas().set(currentMachine.getSymbolicAnimationFormulas().indexOf(item), newItem);
			return true;
		}
		return false;
	}
	
	@Override
	public void checkFormula() {
		SymbolicExecutionType animationType = this.getExecutionType();
		final String formula = extractFormula();
		final SymbolicAnimationItem formulaItem = new SymbolicAnimationItem(formula, animationType);
		addFormula();
		symbolicAnimationItemHandler.handleItem(formulaItem, false);
		this.close();
	}
	
	@Override
	protected void addFormula() {
		SymbolicExecutionType checkingType = this.getExecutionType();
		final String formula = extractFormula();
		final SymbolicAnimationItem formulaItem = new SymbolicAnimationItem(formula, checkingType);
		symbolicAnimationItemHandler.addFormula(formulaItem);
		this.close();
	}
	
	@FXML
	public void cancel() {
		this.close();
	}
}
