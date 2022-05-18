package de.prob2.ui.verifications.symbolicchecking;

import java.util.ResourceBundle;

import javax.inject.Inject;

import de.prob2.ui.internal.AbstractResultHandler;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.symbolic.SymbolicChoosingStage;
import de.prob2.ui.symbolic.SymbolicGUIType;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;

public class SymbolicCheckingChoosingStage extends SymbolicChoosingStage<SymbolicCheckingFormulaItem, SymbolicCheckingType> {
	@FXML
	private TextField idTextField;
	
	@Inject
	private SymbolicCheckingChoosingStage(
		final StageManager stageManager,
		final SymbolicCheckingFormulaHandler symbolicCheckingFormulaHandler,
		final ResourceBundle bundle,
		final CurrentProject currentProject,
		final CurrentTrace currentTrace
	) {
		super(bundle, currentProject, currentTrace, symbolicCheckingFormulaHandler);
		stageManager.loadFXML(this, "symbolic_checking_choice.fxml");
	}
	
	@Override
	public SymbolicGUIType getGUIType(final SymbolicCheckingType item) {
		switch (item) {
			case CHECK_REFINEMENT:
			case CHECK_STATIC_ASSERTIONS:
			case CHECK_DYNAMIC_ASSERTIONS:
			case CHECK_WELL_DEFINEDNESS:
			case FIND_REDUNDANT_INVARIANTS:
				return SymbolicGUIType.NONE;
			
			case INVARIANT:
				return SymbolicGUIType.CHOICE_BOX;
			
			case DEADLOCK:
				return SymbolicGUIType.PREDICATE;
			
			case SYMBOLIC_MODEL_CHECK:
				return SymbolicGUIType.SYMBOLIC_MODEL_CHECK_ALGORITHM;
			
			default:
				throw new AssertionError();
		}
	}
	
	@Override
	protected SymbolicCheckingFormulaItem extractItem() {
		final String id = idTextField.getText().trim().isEmpty() ? null : idTextField.getText();
		return new SymbolicCheckingFormulaItem(id, this.extractFormula(), this.getExecutionType());
	}
	
	@Override
	public void changeFormula(final SymbolicCheckingFormulaItem item, final AbstractResultHandler resultHandler) {
		this.idTextField.setText(item.getId() == null ? "" : item.getId());
		super.changeFormula(item, resultHandler);
	}
}
