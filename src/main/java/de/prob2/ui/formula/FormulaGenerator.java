package de.prob2.ui.formula;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import com.google.inject.Inject;

import de.prob.animator.command.ExpandFormulaCommand;
import de.prob.animator.command.InsertFormulaForVisualizationCommand;
import de.prob.animator.domainobjects.ClassicalB;
import de.prob.animator.domainobjects.EventB;
import de.prob.animator.domainobjects.ExpandedFormula;
import de.prob.animator.domainobjects.FormulaId;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.statespace.AnimationSelector;
import de.prob.statespace.IAnimationChangeListener;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.stage.Stage;



public class FormulaGenerator implements IAnimationChangeListener {

	private Trace currentTrace;
	private IEvalElement formula;
	private FormulaId setFormula;
	private FormulaGraph graph;
	private final StateSpace currentStateSpace;
	private final Set<String> collapsedNodes = new CopyOnWriteArraySet<String>();
	
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
	
	public ExpandedFormula calculateData() {
		if (setFormula != null) {
			ExpandFormulaCommand cmd = new ExpandFormulaCommand(setFormula,
					currentTrace.getCurrentState());
			currentStateSpace.execute(cmd);
			ExpandedFormula result = cmd.getResult();
			result.collapseNodes(new HashSet<String>(collapsedNodes));
			return result;
		}
		return null;
	}
	
	
	public void setFormula(final Map<String, String[]> params) {
		parse(params);
		if (formula == null) {
			System.out.println("Formula = null");
			return;
		}
		if (currentTrace == null) {
			System.out.println("CurrentTrace = null");
			return;
		}
		if (!(formula instanceof EventB || formula instanceof ClassicalB)) {
			System.out.println("ERROR");
			return;
		}
		try {
			InsertFormulaForVisualizationCommand cmd = new InsertFormulaForVisualizationCommand(
					formula);
			currentStateSpace.execute(cmd);
			setFormula = cmd.getFormulaId();
			ExpandedFormula data = calculateData();
			graph = new FormulaGraph(new FormulaNode(25, 350, data));
			draw();
		
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void parse(final Map<String, String[]> params) {
		String f = params.get("formula")[0];
		try {
			IEvalElement e = currentStateSpace.getModel().parseFormula(f);
			formula = e;
		} catch (Exception e) {
			formula = null;
			System.out.println("Parse ERROR");
		}
	}
	
	public void removeFormula(final Map<String, String[]> params) {
		formula = null;
	}

	public void collapseNode(final Map<String, String[]> params) {
		String id = params.get("formulaId")[0];
		collapsedNodes.add(id);
	}

	public void expandNode(final Map<String, String[]> params) {
		String id = params.get("formulaId")[0];
		collapsedNodes.remove(id);
	}

	@Override
	public void traceChange(Trace currentTrace, boolean currentAnimationChanged) {
		/*this.currentTrace = currentTrace;
		if (currentTrace != null
				&& currentTrace.getStateSpace().equals(currentStateSpace)) {
			sendRefresh();
		}*/
		
	}
	
	private void draw() {
		ScrollPane root = new ScrollPane();
		Stage stage = new Stage();
		root.setContent(graph);
		stage.setTitle("Mathematical Expression");
		Scene scene = new Scene(root, 1024, 768);
		stage.setScene(scene);
		stage.show();
	}
	
	@Deprecated
	private void sendRefresh() {
		Object data = calculateData();
	}

	@Override
	public void animatorStatus(boolean busy) {
		// TODO Auto-generated method stub
		
	}
	
	
	
}
