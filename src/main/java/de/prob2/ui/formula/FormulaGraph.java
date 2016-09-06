package de.prob2.ui.formula;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.layout.Region;
import javafx.scene.shape.Line;


public class FormulaGraph extends Region {
	
	private FormulaNode root;
	
	public FormulaGraph(FormulaNode node) {
		root = node;
		root.setPosition(25, calculateY()+20);
		draw(root, 0);
	}
			
	private double calculateY() {
		if(calculateHeight(0)/2 < 400) {
			return 400;
		}
		return calculateHeight(0)/2;
	}
		
	private double calculateHeight(int level) {
		if(level == depth(root)) {
			return 40;
		}
		return Math.max(1,maxChildren(level)) * calculateHeight(level + 1);
	}
			
	private void draw(FormulaNode node, int level) {
		this.getChildren().add(node);
		if(node.next != null) {
			for(int i = 0; i < node.next.size(); i++) {
				double median = (node.next.size()-1)/2.0;
				FormulaNode children = node.next.get(i);
				children.setPosition(node.getRight() + 25 + maxWidth(level + 1) - children.getNodeWidth(), node.getY() + (i - median) * calculateHeight(level+1));
				Line edge1 = new Line(node.getRight(), node.getY(), node.getRight() + 25, children.getY());
				Line edge2 = new Line(node.getRight() + 25, children.getY(), children.getLeft(), children.getY());
				this.getChildren().add(edge1);
				this.getChildren().add(edge2);
				draw(children, level + 1);
			}
		}
	}
	
	private int depth(FormulaNode root) {
		if(root.next == null) {
			return 0;
		}
		int max = 0;
		for(int i = 0; i < root.next.size(); i++) {
			max = Math.max(max, depth(root.next.get(i)));
		}
		return max + 1;
	}
	
	private int breadth(int level) {
		return getAllNodesOnLevel(level).size();
	}
	
	private double maxWidth(int level) {
		double result = 0;
		for(FormulaNode node: getAllNodesOnLevel(level)) {
			result = Math.max(result, node.getNodeWidth());
		}
		return result;
	}
	
	private int maxChildren(int level) {
		if(root.next == null) {
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
		if(level == 0) {
			result.add(root);
			return result;
		}
		
		for(int i = 0; i < getAllNodesOnLevel(level - 1).size(); i++) {
			result.addAll(getAllNodesOnLevel(level - 1).get(i).next);
		}
		return result;
	}

}
