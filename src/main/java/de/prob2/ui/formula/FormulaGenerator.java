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
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseButton;
import javafx.scene.transform.Scale;
import javafx.stage.Stage;

public class FormulaGenerator {
	private IEvalElement formula;
	private FormulaGraph graph;
	private final Set<String> collapsedNodes = new CopyOnWriteArraySet<>();
	
	private final AnimationSelector animationSelector;
	
	private double zoomFactor = 1;
	
	private double oldMousePositionX = -1;
	private double oldMousePositionY = -1;
	
	private Group group;
	private ScrollPane root;
	
	
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
			zoomFactor = 1;		
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
			double Xpos;
			double Ypos;
			double oldWidth = graph.getWidth();
			double oldHeight = graph.getHeight();
			ScrollBar vBar = (ScrollBar) root.getChildrenUnmodifiable().get(1);
			ScrollBar hBar = (ScrollBar) root.getChildrenUnmodifiable().get(2);

			if(e.getButton() == MouseButton.PRIMARY) {
				graph.setPrefHeight(graph.getHeight() * 1.3);
				graph.setPrefWidth(graph.getWidth() * 1.3);
				zoomFactor *= 1.3;
				group.getTransforms().add(new Scale(1.3, 1.3));			
			} else if(e.getButton() == MouseButton.SECONDARY) {
				graph.setPrefHeight(graph.getHeight() * 0.8);
				graph.setPrefWidth(graph.getWidth() * 0.8);
				zoomFactor *= 0.8;
				group.getTransforms().add(new Scale(0.8, 0.8));
				
			}
			group.getChildren().clear();
			group.getChildren().add(graph);

			Xpos = e.getX()*zoomFactor/graph.getWidth();
			Ypos = e.getY()*zoomFactor/graph.getHeight();

			
			root.setHvalue(Xpos);
			root.setVvalue(Ypos);
			
			/*if(e.getX()/oldWidth > 0.9) {
				root.setHvalue(2);
			}
			if(e.getY()/oldHeight > 0.9) {
				root.setVvalue(2);
			}*/
			System.out.println("Real value: " + root.getHvalue() + " " + root.getVvalue());
		});
	}
	
	public void collapseNode(final String id) {
		collapsedNodes.add(id);
	}

	public void expandNode(final String id) {
		collapsedNodes.remove(id);
	}
	
	private void draw() {
		group = new Group();
		root = new ScrollPane(group);
		group.getChildren().add(graph);
		
		Stage stage = new Stage();
		stage.setTitle("Mathematical Expression");
		Scene scene = new Scene(root, 1024, 768);
		
		stage.setScene(scene);
		stage.show();

	}
		
}

