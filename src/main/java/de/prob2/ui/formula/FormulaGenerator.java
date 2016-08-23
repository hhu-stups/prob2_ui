package de.prob2.ui.formula;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import com.google.inject.Inject;
import de.prob.animator.command.ExpandFormulaCommand;
import de.prob.animator.command.InsertFormulaForVisualizationCommand;
import de.prob.animator.domainobjects.ExpandedFormula;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob2.ui.prob2fx.CurrentTrace;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

public class FormulaGenerator {
	
	private final Set<String> collapsedNodes = new CopyOnWriteArraySet<>();
	private final CurrentTrace currentTrace;

	@Inject
	public FormulaGenerator(final CurrentTrace currentTrace) {
		this.currentTrace = currentTrace;
	}
	
	public void setFormula(final IEvalElement formula) {
		try {
			InsertFormulaForVisualizationCommand cmd1 = new InsertFormulaForVisualizationCommand(formula);
			if(!currentTrace.get().getCurrentState().isInitialised()) {
				showFormulaViewError(FormulaViewErrorType.NOT_INITIALIZE_ERROR);
				return;
			}
			currentTrace.getStateSpace().execute(cmd1);
			ExpandFormulaCommand cmd2 = new ExpandFormulaCommand(cmd1.getFormulaId(), currentTrace.get().getCurrentState());
			currentTrace.getStateSpace().execute(cmd2);
			ExpandedFormula data = cmd2.getResult();
			data.collapseNodes(new HashSet<>(collapsedNodes));
			FormulaGraph graph = new FormulaGraph(new FormulaNode(data));
			FormulaView fview = new FormulaView(graph);
			fview.show();
		} catch (Exception e) {
			showFormulaViewError(FormulaViewErrorType.PARSING_ERROR);
		}
	}
	
	private void showFormulaViewError(FormulaViewErrorType error) {
		Alert alert = new Alert(AlertType.ERROR);
		alert.setTitle("Error while trying to visualize expression");
		alert.setHeaderText("The current state is not initialized.");
		if(error == FormulaViewErrorType.PARSING_ERROR) {
			alert.setHeaderText("The formula cannot be parsed and visualize.");
		}
		alert.getDialogPane().getStylesheets().add("prob.css");
		alert.showAndWait();
	}
	
	
	public IEvalElement parse(String params) {
		try {
			IEvalElement formula = currentTrace.getModel().parseFormula(params);
			return formula;
		} catch (Exception e) {
			return null;
		}
	}
	
	public void collapseNode(final String id) {
		collapsedNodes.add(id);
	}

	public void expandNode(final String id) {
		collapsedNodes.remove(id);
	}
}

