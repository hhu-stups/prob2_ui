package de.prob2.ui.dotty;

import javafx.scene.layout.Region;

public class FormulaGraph extends Region {

	private FormulaNode root;
	
	
	public FormulaGraph(double centerX, double centerY, String data) {
		this.root = new FormulaNode(centerX, centerY, data);
		this.getChildren().add(root);
	}
	
	public void add(FormulaNode node) {
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
		FormulaNode current = root;
		this.getChildren().add(root);
		while(current.next != null) {
			current = current.next;
			System.out.println(current);
			this.getChildren().add(current);
		}
	}
	
}
