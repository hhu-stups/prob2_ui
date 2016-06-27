package de.prob2.ui.modelchecking;

import javafx.geometry.Pos;
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
		nodes[0] = toFX(name);
		nodes[1] = toFX(value);
		
		return nodes;
	}

	private Node toFX(String s) {
		AnchorPane aP = new AnchorPane();
		aP.setMinHeight(30.0);
		aP.getStylesheets().add("prob.css");
		aP.getStyleClass().add("gridViewRow");
		Label l = new Label();
		l.setText(s);
//		l.setAlignment(Pos.CENTER_LEFT);
		aP.getChildren().add(l);
		AnchorPane.setLeftAnchor(l, 10.0);
		AnchorPane.setTopAnchor(l, 6.0);
		AnchorPane.setBottomAnchor(l, 7.5);
	
		return aP;
	}

}
