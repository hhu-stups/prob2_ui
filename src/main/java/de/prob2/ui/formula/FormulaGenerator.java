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
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseButton;
import javafx.scene.transform.Scale;
import javafx.stage.Stage;

public class FormulaGenerator {
	private FormulaGraph graph;
	private final Set<String> collapsedNodes = new CopyOnWriteArraySet<>();
	
	private final AnimationSelector animationSelector;
		
	private double oldMousePositionX = -1;
	private double oldMousePositionY = -1;
	
	private Group group;
	private ScrollPane root;
	private Stage stage;
	
	
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
			setEventListeners();
			draw();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void setEventListeners() {
		graph.setOnMouseMoved(e-> {
			graph.setCursor(Cursor.HAND);
			oldMousePositionX = e.getSceneX();
			oldMousePositionY = e.getSceneY();
		});
		
		graph.setOnMouseDragged(e-> {
			graph.setCursor(Cursor.MOVE);
			root.setHvalue(root.getHvalue() + (-e.getSceneX() + oldMousePositionX)/graph.getWidth());
			root.setVvalue(root.getVvalue() + (-e.getSceneY() + oldMousePositionY)/graph.getHeight());
			oldMousePositionX = e.getSceneX();
			oldMousePositionY = e.getSceneY();

		});
		graph.setOnMouseClicked(e -> {
			if(e.getClickCount() < 2) {
				return;
			}

			if(e.getButton() == MouseButton.PRIMARY) {
				graph.getTransforms().add(new Scale(1.3, 1.3));			
			} else if(e.getButton() == MouseButton.SECONDARY) {
				graph.getTransforms().add(new Scale(0.8, 0.8));
				
			}
			
			group.getChildren().clear();
			group.getChildren().add(graph);
			double Xpos = e.getX()/graph.getWidth();
			double Ypos = e.getY()/graph.getHeight();
			root.setHvalue(Xpos);
			root.setVvalue(Ypos);
		});
	}
	
	public void collapseNode(final String id) {
		collapsedNodes.add(id);
	}

	public void expandNode(final String id) {
		collapsedNodes.remove(id);
	}
	
	private void draw() {
		if(stage != null) { 
			stage.close();
		}
		group = new Group();
		root = new ScrollPane(group);
		group.getChildren().add(graph);
		
		stage = new Stage();
		stage.setTitle("Mathematical Expression");
		Scene scene = new Scene(root, 1024, 768);
		
		stage.setScene(scene);
		stage.show();

	}
		
}

