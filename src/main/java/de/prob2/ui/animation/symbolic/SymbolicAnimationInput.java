package de.prob2.ui.animation.symbolic;

import java.util.ResourceBundle;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.symbolic.SymbolicChoosingStage;
import de.prob2.ui.symbolic.SymbolicExecutionType;
import de.prob2.ui.symbolic.SymbolicFormulaInput;
import de.prob2.ui.symbolic.SymbolicGUIType;

import javafx.fxml.FXML;

@FXMLInjected
@Singleton
public class SymbolicAnimationInput extends SymbolicFormulaInput<SymbolicAnimationItem> {
	
	
	private final SymbolicAnimationItemHandler symbolicAnimationItemHandler;

	@Inject
	private SymbolicAnimationInput(final StageManager stageManager, final CurrentProject currentProject, final Injector injector, final ResourceBundle bundle,
										 final CurrentTrace currentTrace, final SymbolicAnimationItemHandler symbolicAnimationItemHandler) {
		super(currentProject, injector, bundle, currentTrace);
		this.symbolicAnimationItemHandler = symbolicAnimationItemHandler;
		stageManager.loadFXML(this, "symbolic_animation_formula_input.fxml");
	}

	@Override
	protected boolean updateFormula(SymbolicAnimationItem item, SymbolicChoosingStage<SymbolicAnimationItem> choosingStage) {
		Machine currentMachine = currentProject.getCurrentMachine();
		String formula = extractFormula(choosingStage);

		SymbolicAnimationItem newItem = new SymbolicAnimationItem(formula, choosingStage.getExecutionType());
		if(currentMachine.getSymbolicAnimationFormulas().stream().noneMatch(newItem::settingsEqual)) {
			currentMachine.getSymbolicAnimationFormulas().set(currentMachine.getSymbolicAnimationFormulas().indexOf(item), newItem);
			return true;
		}
		return false;
	}

	@Override
	public void checkFormula() {
		SymbolicExecutionType animationType = injector.getInstance(SymbolicAnimationChoosingStage.class).getExecutionType();
		final String formula = extractFormula(injector.getInstance(SymbolicAnimationChoosingStage.class));
		final SymbolicAnimationItem formulaItem = new SymbolicAnimationItem(formula, animationType);
		addFormula();
		switch (animationType) {
			case SEQUENCE:
				symbolicAnimationItemHandler.handleSequence(formulaItem, false);
				break;
			case FIND_VALID_STATE:
				symbolicAnimationItemHandler.findValidState(formulaItem, false);
				break;
			default:
				break;
		}
		injector.getInstance(SymbolicAnimationChoosingStage.class).close();
	}

	@Override
	protected void addFormula() {
		SymbolicExecutionType checkingType = injector.getInstance(SymbolicAnimationChoosingStage.class).getExecutionType();
		final String formula = extractFormula(injector.getInstance(SymbolicAnimationChoosingStage.class));
		final SymbolicAnimationItem formulaItem = new SymbolicAnimationItem(formula, checkingType);
		symbolicAnimationItemHandler.addFormula(formulaItem);
		injector.getInstance(SymbolicAnimationChoosingStage.class).close();
	}
		
	@FXML
	public void cancel() {
		injector.getInstance(SymbolicAnimationChoosingStage.class).close();
	}

}
