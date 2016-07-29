package de.prob2.ui.formula;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.layout.Region;
import javafx.scene.shape.Line;


public class FormulaGraph extends Region {
	
	private FormulaNode root;
	
	public FormulaGraph(FormulaNode node) {
		 root = node;
		 root.setPosition(25, calculateY());
		 draw(root, 0);
	}
	
	private double calculateY() {
		double width = calculateWidth(root, 0);
		if (width < 800) {
			return 400;
		}
		return width/2;
	}
	
	private double calculateWidth(FormulaNode root, int level) {
		if(root.next == null) {
			return 15;
		}
		double result = 0;
		for(int i = 0; i < root.next.size(); i++) {
			result = result + depth(root) * maxChildren(level) + calculateWidth(root.next.get(i), level + 1);
		}
		return result + 15;
	}
	
	private void draw(FormulaNode node, int level) {
		FormulaNode current = node;
		this.getChildren().add(current);
		if(current.next != null) {
			for(int i = 0; i < current.next.size(); i++) {
				double median = (current.next.size()-1)/2.0;
				FormulaNode children = current.next.get(i);
				children.setPosition(current.getRight() + 25, current.getY() + (i - median) * (15 * depth(node)  * maxChildren(level) + calculateWidth(current.next.get(i), level)));
				Line edge = new Line(current.getRight(), current.getY(), children.getLeft(), children.getY());
				this.getChildren().add(edge);
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
	
	private int maxChildren(int level) {
		if(root.next == null) {
			return 0;
		}
		
		List<FormulaNode> nodesOnLevel = getAllNodesOnLevel(level);
		int max = 0;
		
		for(int i = 0; i < nodesOnLevel.size(); i++) {
			max = Math.max(max, nodesOnLevel.get(i).next.size());
		}
		return max;
	}
	
	private List<FormulaNode> getAllNodesOnLevel (int level) {
		List<FormulaNode> result = new ArrayList<FormulaNode>();
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
