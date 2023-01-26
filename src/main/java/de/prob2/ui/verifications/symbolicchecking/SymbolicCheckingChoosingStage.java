package de.prob2.ui.verifications.symbolicchecking;

import javax.inject.Inject;

import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
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
		final I18n i18n,
		final CurrentTrace currentTrace
	) {
		super(i18n, currentTrace);
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
	public void setData(final SymbolicCheckingFormulaItem item) {
		super.setData(item);
		this.idTextField.setText(item.getId() == null ? "" : item.getId());
	}
}
