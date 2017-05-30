package de.prob2.ui.stats;

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
		return new Node[] {toFX(name, false), toFX(value, true)};
	}

	private Node toFX(String s, boolean rightAligned) {
		AnchorPane aP = new AnchorPane();
		aP.getStyleClass().add("gridViewRow");
		Label l = new Label(s);
		l.setTooltip(new Tooltip(s));
		aP.getChildren().add(l);
		AnchorPane.setTopAnchor(l, 6.0);
		AnchorPane.setBottomAnchor(l, 7.5);
		if (rightAligned) {
			AnchorPane.setRightAnchor(l, 10.0);
		} else {
			AnchorPane.setLeftAnchor(l, 10.0);
			AnchorPane.setRightAnchor(l, 0.0);
		}
	
		return aP;
	}

}
