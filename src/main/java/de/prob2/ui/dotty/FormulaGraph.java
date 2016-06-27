package de.prob2.ui.dotty;

import java.util.List;

import javafx.scene.layout.Region;
import javafx.scene.shape.Line;


public class FormulaGraph extends Region {

	private FormulaNode root;
	
	
	public FormulaGraph(FormulaNode node) {
		this.root = node;
		//this.getChildren().add(root);
		show(root);
		System.out.println(depth(root));
	}
	
	
	/*
	
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
	}*/
	
	private void show(FormulaNode node) {
		//has to be changed
		/*FormulaNode current = root;
		this.getChildren().add(root);
		while(current.next != null) {
			Line edge = new Line(current.getRight(), current.getY(), current.next.getLeft(), current.next.getY());
			current = current.next;
			this.getChildren().add(edge);
			this.getChildren().add(current);
		}*/
		
		FormulaNode current = node;
		this.getChildren().add(current);
		if(!current.next.isEmpty()) {
			for(int i = 0; i < current.next.size(); i++) {

				int median = current.next.size()/2;
				FormulaNode children = current.next.get(i);
				children.setPosition(current.getRight() + 100, current.getY() + (i - median) * 50 * depth(current));
				Line edge = new Line(current.getRight(), current.getY(), 
						 current.next.get(i).getLeft(), current.next.get(i).getY());
				this.getChildren().add(edge);
				show(children);
								
			}
		}
		
	}
	
	public int depth(FormulaNode root) {
		if(root.next.isEmpty()) {
			return 0;
		}
		int max = 0;
		for(int i = 0; i < root.next.size(); i++) {
			max = Math.max(max, depth(root.next.get(i)));
		}
		return max + 1;
	}

	
}
