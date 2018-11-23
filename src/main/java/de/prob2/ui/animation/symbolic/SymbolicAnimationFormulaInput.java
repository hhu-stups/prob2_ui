package de.prob2.ui.animation.symbolic;

import java.util.ResourceBundle;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.symbolic.SymbolicExecutionType;
import de.prob2.ui.symbolic.SymbolicFormulaInput;
import de.prob2.ui.symbolic.SymbolicGUIType;

import javafx.fxml.FXML;

@FXMLInjected
@Singleton
public class SymbolicAnimationFormulaInput extends SymbolicFormulaInput<SymbolicAnimationFormulaItem> {
	
	
	private final SymbolicAnimationFormulaHandler symbolicAnimationFormulaHandler;
	
	@Inject
	public SymbolicAnimationFormulaInput(final StageManager stageManager, 
										final SymbolicAnimationFormulaHandler symbolicAnimationFormulaHandler,
										final CurrentProject currentProject, final Injector injector, final ResourceBundle bundle,
										final CurrentTrace currentTrace) {
		super(stageManager, currentProject, injector, bundle, currentTrace);
		this.symbolicAnimationFormulaHandler = symbolicAnimationFormulaHandler;
		stageManager.loadFXML(this, "symbolic_animation_formula_input.fxml");
	}
	
	protected void setCheckListeners() {
		btAdd.setOnAction(e -> addFormula(false));
		btCheck.setOnAction(e -> {
			SymbolicExecutionType animationType = injector.getInstance(SymbolicAnimationChoosingStage.class).getExecutionType();
			SymbolicAnimationFormulaItem formulaItem = null;
			addFormula(true);
			switch (animationType) {
			case SEQUENCE:
				symbolicAnimationFormulaHandler.handleSequence(tfFormula.getText(), false);
				break;
			case FIND_VALID_STATE:
				formulaItem = new SymbolicAnimationFormulaItem(predicateBuilderView.getPredicate(),
						SymbolicExecutionType.FIND_VALID_STATE);
				symbolicAnimationFormulaHandler.findValidState(formulaItem, false);
				break;
			default:
				break;
			}
			injector.getInstance(SymbolicAnimationChoosingStage.class).close();
		});
	}

	private void addFormula(boolean checking) {
		SymbolicExecutionType checkingType = injector.getInstance(SymbolicAnimationChoosingStage.class).getExecutionType();
		SymbolicGUIType guiType = injector.getInstance(SymbolicAnimationChoosingStage.class).getGUIType();
		switch(guiType) {
			case TEXT_FIELD:
				symbolicAnimationFormulaHandler.addFormula(tfFormula.getText(), checkingType, checking);
				break;
			case PREDICATE:
				final String predicate = predicateBuilderView.getPredicate();
				symbolicAnimationFormulaHandler.addFormula(predicate, checkingType, checking);
				break;
			case NONE:
				symbolicAnimationFormulaHandler.addFormula(checkingType.name(), checkingType, checking);
				break;
			default:
				break;
		}
		injector.getInstance(SymbolicAnimationChoosingStage.class).close();
	}
		
	@FXML
	public void cancel() {
		injector.getInstance(SymbolicAnimationChoosingStage.class).close();
	}

}
