package de.prob2.ui.formula;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob.animator.command.ExpandFormulaCommand;
import de.prob.animator.command.InsertFormulaForVisualizationCommand;
import de.prob.animator.domainobjects.EvaluationException;
import de.prob.animator.domainobjects.ExpandedFormula;
import de.prob.animator.domainobjects.IEvalElement;

import de.prob2.ui.prob2fx.CurrentStage;
import de.prob2.ui.prob2fx.CurrentTrace;

@Singleton
public final class FormulaGenerator {
	
	private final CurrentTrace currentTrace;
	private final CurrentStage currentStage;


	@Inject
	private FormulaGenerator(final CurrentTrace currentTrace, final CurrentStage currentStage) {
		this.currentTrace = currentTrace;
		this.currentStage = currentStage;
	}

	private ExpandedFormula expandFormula(final IEvalElement formula) {
		if (!currentTrace.getCurrentState().isInitialised()) {
			throw new EvaluationException("Formula evaluation is only possible in an initialized state");
		}

		final InsertFormulaForVisualizationCommand insertCmd = new InsertFormulaForVisualizationCommand(formula);
		currentTrace.getStateSpace().execute(insertCmd);

		final ExpandFormulaCommand expandCmd = new ExpandFormulaCommand(insertCmd.getFormulaId(),
				currentTrace.getCurrentState());
		currentTrace.getStateSpace().execute(expandCmd);

		return expandCmd.getResult();
	}

	public void showFormula(final IEvalElement formula) {
		FormulaView fview = new FormulaView(new FormulaGraph(new FormulaNode(expandFormula(formula))));
		currentStage.register(fview);
		fview.show();
	}

	public void parseAndShowFormula(final String formula) {
		showFormula(currentTrace.getModel().parseFormula(formula));
	}
}
