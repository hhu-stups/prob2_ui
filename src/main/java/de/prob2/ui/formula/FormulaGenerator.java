package de.prob2.ui.formula;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.prob.animator.command.ExpandFormulaCommand;
import de.prob.animator.command.InsertFormulaForVisualizationCommand;
import de.prob.animator.domainobjects.EvaluationException;
import de.prob.animator.domainobjects.ExpandedFormula;
import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob2.ui.prob2fx.CurrentTrace;

@Singleton
public final class FormulaGenerator {
	
	private final CurrentTrace currentTrace;

	@Inject
	private FormulaGenerator(final CurrentTrace currentTrace) {
		this.currentTrace = currentTrace;
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

	public FormulaView showFormula(final IEvalElement formula) {
		return new FormulaView(new FormulaGraph(new FormulaNode(expandFormula(formula))));
	}

	public FormulaView parseAndShowFormula(final String formula) {
		return showFormula(currentTrace.getModel().parseFormula(formula, FormulaExpand.EXPAND));
	}
}
