package de.prob2.ui.dotty;

import java.util.List;

import javafx.scene.layout.Region;
import javafx.scene.shape.Line;


public class FormulaGraph extends Region {

	private FormulaNode root;
	
	
	public FormulaGraph(double centerX, double centerY, String data) {
		this.root = new FormulaNode(centerX, centerY, data);
		this.getChildren().add(root);
	}
	
	
	
	
	public void add(FormulaNode node) {
		//has to be changed
		FormulaNode current = root;
		while(current.next != null) {
			current = current.next;
		}
		node.setPosition(current.getRight() + 100, current.getY());
		current.next = node;
		this.getChildren().clear();
		show();
	}
	
	public void show() {
		//has to be changed
		FormulaNode current = root;
		this.getChildren().add(root);
		while(current.next != null) {
			Line edge = new Line(current.getRight(), current.getY(), current.next.getLeft(), current.next.getY());
			current = current.next;
			this.getChildren().add(edge);
			this.getChildren().add(current);
		}
	}

	
}
