package de.prob2.ui.formula;

import javafx.scene.layout.Region;
import javafx.scene.shape.Line;


public class FormulaGraph extends Region {
	
	private FormulaNode root;
	
	public FormulaGraph(FormulaNode node) {
		 this.root = node;
		 draw(root);
	}
	
	private void draw(FormulaNode node) {
		FormulaNode current = node;
		this.getChildren().add(current);
		if(current.next != null) {
			for(int i = 0; i < current.next.size(); i++) {
				double median = (current.next.size()-1)/2.0;
				FormulaNode children = current.next.get(i);
				children.setPosition(current.getRight() + 25, current.getY() + (i - median) * 10 * node.next.size() * depth(current));
				Line edge = new Line(current.getRight(), current.getY(), children.getLeft(), children.getY());
				this.getChildren().add(edge);
				draw(children);				
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
	
	
	

}
