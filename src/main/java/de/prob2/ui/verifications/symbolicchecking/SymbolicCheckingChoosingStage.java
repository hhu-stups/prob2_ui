package de.prob2.ui.verifications.symbolicchecking;

import java.util.ResourceBundle;

import javax.inject.Inject;

import com.google.inject.Singleton;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.symbolic.SymbolicChoosingStage;

public class SymbolicCheckingChoosingStage extends SymbolicChoosingStage<SymbolicCheckingFormulaItem> {
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
	protected SymbolicCheckingFormulaItem extractItem() {
		return new SymbolicCheckingFormulaItem(this.extractFormula(), this.getExecutionType());
	}
}
