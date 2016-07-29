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
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.stage.Stage;

public class FormulaGenerator {
	private IEvalElement formula;
	private FormulaGraph graph;
	private final Set<String> collapsedNodes = new CopyOnWriteArraySet<>();
	
	private final AnimationSelector animationSelector;
	
	@Inject
	public FormulaGenerator(final AnimationSelector animationSelector) {
		this.animationSelector = animationSelector;
	}
	
	public void setFormula(final IEvalElement formula) {
		try {
			InsertFormulaForVisualizationCommand cmd1 = new InsertFormulaForVisualizationCommand(formula);
			animationSelector.getCurrentTrace().getStateSpace().execute(cmd1);
			
			ExpandFormulaCommand cmd2 = new ExpandFormulaCommand(cmd1.getFormulaId(), animationSelector.getCurrentTrace().getCurrentState());
			animationSelector.getCurrentTrace().getStateSpace().execute(cmd2);
			ExpandedFormula data = cmd2.getResult();
			data.collapseNodes(new HashSet<>(collapsedNodes));
			
			//graph = new FormulaGraph(new FormulaNode(25, 350, data));
			graph = new FormulaGraph(new FormulaNode(data));
			draw();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void collapseNode(final String id) {
		collapsedNodes.add(id);
	}

	public void expandNode(final String id) {
		collapsedNodes.remove(id);
	}
	
	private void draw() {
		ScrollPane root = new ScrollPane(graph);
		//root.setPrefSize(graph.getPrefWidth(), graph.getPrefHeight()+100);
		Stage stage = new Stage();
		stage.setTitle("Mathematical Expression");
		Scene scene = new Scene(root, 1024, 768);
		//Scene scene = new Scene(root);
		stage.setScene(scene);
		stage.show();
	}
}
