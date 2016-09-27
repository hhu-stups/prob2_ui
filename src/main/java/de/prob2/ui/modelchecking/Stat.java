package de.prob2.ui.modelchecking;

import java.util.Objects;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.AnchorPane;

public class Stat {

	private final String name;
	private final String value;

	public Stat(String name, String value) {
		Objects.requireNonNull(name);
		Objects.requireNonNull(value);
		
		this.name = name;
		this.value = value;
	}
	
	public Stat(String name) {
		this(name, "");
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
		l.setTooltip(new Tooltip(s));
		aP.getChildren().add(l);
		AnchorPane.setTopAnchor(l, 6.0);
		AnchorPane.setBottomAnchor(l, 7.5);
		if("left".equals(alignment)) {
			AnchorPane.setLeftAnchor(l, 10.0);
			AnchorPane.setRightAnchor(l, 0.0);
		} else if("right".equals(alignment)) {
			AnchorPane.setRightAnchor(l, 10.0);
		}
	
		return aP;
	}

}
