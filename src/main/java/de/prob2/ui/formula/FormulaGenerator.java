package de.prob2.ui.formula;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import com.google.inject.Inject;

import de.prob.animator.command.ExpandFormulaCommand;
import de.prob.animator.command.InsertFormulaForVisualizationCommand;
import de.prob.animator.domainobjects.ExpandedFormula;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.statespace.AnimationSelector;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

public class FormulaGenerator {
	
	private final Set<String> collapsedNodes = new CopyOnWriteArraySet<>();
	private final AnimationSelector animationSelector;

	@Inject
	public FormulaGenerator(final AnimationSelector animationSelector) {
		this.animationSelector = animationSelector;
	}
	
	public void setFormula(final IEvalElement formula) {
		try {
			InsertFormulaForVisualizationCommand cmd1 = new InsertFormulaForVisualizationCommand(formula);
			Trace currentTrace = animationSelector.getCurrentTrace();
			if(!currentTrace.canGoBack()) {
				showFormulaViewError(FormulaViewErrorType.NOT_INITIALIZE_ERROR);
				return;
			}
			currentTrace.getStateSpace().execute(cmd1);
			ExpandFormulaCommand cmd2 = new ExpandFormulaCommand(cmd1.getFormulaId(), currentTrace.getCurrentState());
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
		alert.setHeaderText("The statespace is not initialized.");
		if(error == FormulaViewErrorType.PARSING_ERROR) {
			alert.setHeaderText("The formula cannot be parsed and visualize.");
		}
		alert.showAndWait();
	}
	
	
	public IEvalElement parse(String params) {
		try {
			StateSpace currentStateSpace = animationSelector.getCurrentTrace().getStateSpace();
			IEvalElement formula = currentStateSpace.getModel().parseFormula(params);
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

