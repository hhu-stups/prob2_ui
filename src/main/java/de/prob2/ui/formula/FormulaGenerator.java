package de.prob2.ui.formula;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.prob.animator.command.ExpandFormulaCommand;
import de.prob.animator.command.InsertFormulaForVisualizationCommand;
import de.prob.animator.domainobjects.EvaluationException;
import de.prob.animator.domainobjects.ExpandedFormula;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.exception.ProBError;
import de.prob2.ui.prob2fx.CurrentTrace;
import javafx.scene.control.Alert;

@Singleton
public final class FormulaGenerator {
	private final CurrentTrace currentTrace;
	
	@Inject
	private FormulaGenerator(final CurrentTrace currentTrace) {
		this.currentTrace = currentTrace;
	}
	
	private ExpandedFormula expandFormula(final IEvalElement formula) {
		final InsertFormulaForVisualizationCommand insertCmd = new InsertFormulaForVisualizationCommand(formula);
		currentTrace.getStateSpace().execute(insertCmd);
		
		final ExpandFormulaCommand expandCmd = new ExpandFormulaCommand(insertCmd.getFormulaId(), currentTrace.get().getCurrentState());
		currentTrace.getStateSpace().execute(expandCmd);
		
		return expandCmd.getResult();
	}
	
	public void showFormula(final IEvalElement formula) {
		try {
			if (!currentTrace.get().getCurrentState().isInitialised()) {
				// noinspection ThrowCaughtLocally
				throw new EvaluationException("Formula evaluation is only possible in an initialized state");
			}
			
			ExpandedFormula expanded = expandFormula(formula);
			FormulaView fview = new FormulaView(new FormulaGraph(new FormulaNode(expanded)));
			fview.show();
		} catch (EvaluationException | ProBError e) {
			e.printStackTrace();
			final Alert alert = new Alert(Alert.AlertType.ERROR, "Could not visualize formula:\n" + e);
			alert.getDialogPane().getStylesheets().add("prob.css");
			alert.showAndWait();
		}
	}
	
	public void parseAndShowFormula(final String formula) {
		final IEvalElement parsed;
		try {
			parsed = currentTrace.getModel().parseFormula(formula);
		} catch (EvaluationException e) {
			e.printStackTrace();
			final Alert alert = new Alert(Alert.AlertType.ERROR, "Could not parse formula:\n" + e);
			alert.getDialogPane().getStylesheets().add("prob.css");
			alert.showAndWait();
			return;
		}
		showFormula(parsed);
	}
}

