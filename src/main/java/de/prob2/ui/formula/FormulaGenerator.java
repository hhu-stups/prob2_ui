package de.prob2.ui.formula;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import com.google.inject.Inject;
import de.prob.animator.command.ExpandFormulaCommand;
import de.prob.animator.command.InsertFormulaForVisualizationCommand;
import de.prob.animator.domainobjects.ExpandedFormula;
import de.prob.animator.domainobjects.FormulaId;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.statespace.AnimationSelector;
import de.prob.statespace.IAnimationChangeListener;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.stage.Stage;



public class FormulaGenerator implements IAnimationChangeListener {

	private Trace currentTrace;
	private IEvalElement formula;
	private FormulaId setFormula;
	private FormulaGraph graph;
	private final StateSpace currentStateSpace;
	private final Set<String> collapsedNodes = new CopyOnWriteArraySet<>();
	
	private final AnimationSelector animations;
	
	@Inject
	public FormulaGenerator(final AnimationSelector animations) {
		this.animations = animations;
		currentTrace = animations.getCurrentTrace();
		if (currentTrace == null) {
			currentStateSpace = null;
		} else {
			currentStateSpace = currentTrace.getStateSpace();
			this.animations.registerAnimationChangeListener(this);
		}
	}
	
	public void setFormula(final IEvalElement formula) {
		try {
			InsertFormulaForVisualizationCommand cmd1 = new InsertFormulaForVisualizationCommand(formula);
			currentStateSpace.execute(cmd1);
			setFormula = cmd1.getFormulaId();
			
			ExpandFormulaCommand cmd2 = new ExpandFormulaCommand(setFormula, currentTrace.getCurrentState());
			currentStateSpace.execute(cmd2);
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
	
	@Override
	public void traceChange(Trace currentTrace, boolean currentAnimationChanged) {}
	
	@Override
	public void animatorStatus(boolean busy) {}
}
