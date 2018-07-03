package de.prob2.ui.formula;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleExpression;
import javafx.scene.layout.Region;
import javafx.scene.shape.Line;

import java.util.ArrayList;
import java.util.List;

import de.prob2.ui.layout.FontSize;

public class FormulaGraph extends Region {
	
	private FormulaNode root;
	
	private final FontSize fontSize;
	
	public FormulaGraph(FormulaNode node, FontSize fontSize) {
		this.root = node;
		this.fontSize = fontSize;
		root.setPosition(25, calculateY()+20);
		draw(root, 0);
	}
	
	private double calculateY() {
		if (calculateHeight(0)/2 < 400) {
			return 400;
		}
		return calculateHeight(0)/2;
	}
	
	private double calculateHeight(int level) {
		if (level == depth(root)) {
			return 40 * ((double) fontSize.getFontSize())/FontSize.DEFAULT_FONT_SIZE;
		}
		return Math.max(1,maxChildren(level)) * calculateHeight(level + 1);
	}
	
	private void draw(FormulaNode node, int level) {
		this.getChildren().add(node);
		if (node.next != null) {
			for (int i = 0; i < node.next.size(); i++) {
				double median = (node.next.size()-1)/2.0;
				FormulaNode children = node.next.get(i);
				children.layoutXProperty().bind(node.rightProperty().add(25).add(maxWidthProperty(level + 1)).subtract(children.prefWidthProperty()));
				children.layoutYProperty().bind(node.layoutYProperty().add((i - median) * calculateHeight(level+1)));
				Line edge1 = new Line();
				Line edge2 = new Line();
				edge1.startXProperty().bind(node.rightProperty());
				edge1.startYProperty().bind(node.yProperty());
				edge1.endXProperty().bind(node.rightProperty().add(25));
				edge1.endYProperty().bind(children.yProperty());
				
				edge2.startXProperty().bind(node.rightProperty().add(25));
				edge2.startYProperty().bind(children.yProperty());
				edge2.endXProperty().bind(children.leftProperty());
				edge2.endYProperty().bind(children.yProperty());

				this.getChildren().add(edge1);
				this.getChildren().add(edge2);
				draw(children, level + 1);
			}
		}
	}
	
	private int depth(FormulaNode root) {
		if (root.next == null) {
			return 0;
		}
		int max = 0;
		
		for (int i = 0; i < root.next.size(); i++) {
			max = Math.max(max, depth(root.next.get(i)));
		}
		return max + 1;
	}
	
	public DoubleExpression maxWidthProperty(int level) {
		return getAllNodesOnLevel(level).stream()
			.map(node -> (DoubleExpression) node.prefWidthProperty())
			.reduce((a,e) -> (DoubleExpression) Bindings.max(a,e))
			.get();
	}
	
	private int maxChildren(int level) {
		if (root.next == null) {
			return 0;
		}
		
		List<FormulaNode> nodesOnLevel = getAllNodesOnLevel(level);
		int max = 0;
		
		for (FormulaNode aNodesOnLevel : nodesOnLevel) {
			max = Math.max(max, aNodesOnLevel.next.size());
		}
		return max;
	}
	
	private List<FormulaNode> getAllNodesOnLevel (int level) {
		List<FormulaNode> result = new ArrayList<>();
		if (level == 0) {
			result.add(root);
			return result;
		}
		
		for (int i = 0; i < getAllNodesOnLevel(level - 1).size(); i++) {
			result.addAll(getAllNodesOnLevel(level - 1).get(i).next);
		}
		return result;
	}

}
