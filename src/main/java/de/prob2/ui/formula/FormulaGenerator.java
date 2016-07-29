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
import javafx.scene.input.MouseButton;
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
			
			graph = new FormulaGraph(new FormulaNode(data));
			graph.autosize();
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
		Stage stage = new Stage();
		stage.setTitle("Mathematical Expression");
		Scene scene = new Scene(root, 1024, 768);
		root.setOnMouseClicked(e -> {
			if(e.getButton() == MouseButton.PRIMARY) {
				
				graph.setPrefHeight(graph.getPrefHeight() * 2);
				graph.setPrefWidth(graph.getPrefWidth() * 2);
				graph.setScaleX(graph.getScaleX() * 2);
				graph.setScaleY(graph.getScaleY() * 2);

			} else if(e.getButton() == MouseButton.SECONDARY) {
				graph.setPrefHeight(graph.getPrefHeight() * 0.5);
				graph.setPrefWidth(graph.getPrefWidth() * 0.5);
				
				graph.setScaleX(graph.getScaleX() * 0.5);
				graph.setScaleY(graph.getScaleY() * 0.5);
			}
			root.setContent(graph);
			//scene.setRoot(root);
			//stage.show();
		});
		stage.setScene(scene);
		stage.show();
	}
}
