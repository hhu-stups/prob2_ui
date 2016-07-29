package de.prob2.ui.modelchecking;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;

public class Stat {

	private String name;
	private String value;

	public Stat(String name, String value) {
		this.name = name;
		this.value = value != null ? value : "";
	}

	public Node[] toFX() {
		Node[] nodes = new Node[2];
		nodes[0] = toFX(name, "left");
		nodes[1] = toFX(value, "right");
		
		return nodes;
	}

	private Node toFX(String s, String alignment) {
		AnchorPane aP = new AnchorPane();
		aP.setMinHeight(30.0);
		aP.getStylesheets().add("prob.css");
		aP.getStyleClass().add("gridViewRow");
		Label l = new Label();
		l.setText(s);
		aP.getChildren().add(l);
		AnchorPane.setTopAnchor(l, 6.0);
		AnchorPane.setBottomAnchor(l, 7.5);
		if(alignment.equals("left")) {
			AnchorPane.setLeftAnchor(l, 10.0);
		} else if(alignment.equals("right")) {
			AnchorPane.setRightAnchor(l, 10.0);
		}
	
		return aP;
	}

}
