package de.prob2.ui.formula;

import de.prob.animator.domainobjects.ExpandedFormula;
import de.prob2.ui.layout.FontSize;
import javafx.beans.binding.DoubleExpression;
import javafx.scene.control.Label;


import java.util.ArrayList;
import java.util.List;


public class FormulaNode extends Label {
	
	List<FormulaNode> next;
	private final FontSize fontSize;
	
	public FormulaNode(ExpandedFormula data, FontSize fontSize) {
		this.fontSize = fontSize;
		this.next = new ArrayList<>();
		String text = data.getLabel();
		if (data.getValue() instanceof String) {
			text = text + " = " + data.getValue();
		}
		this.setText(text);
		calculateColor(data);
		if (data.getChildren() == null || data.getChildren().isEmpty()) {
			return;
		}
		for (int i = 0; i < data.getChildren().size(); i++) {
			next.add(new FormulaNode(data.getChildren().get(i), fontSize));
		}
	}
	
	
	public void setPosition(double x, double y) {
		this.setLayoutX(x + getWidth() * ((double) fontSize.getFontSize())/FontSize.DEFAULT_FONT_SIZE/2);
		this.setLayoutY(y - getHeight() * ((double) fontSize.getFontSize())/FontSize.DEFAULT_FONT_SIZE);
	}
	
	public DoubleExpression leftProperty() {
		return this.layoutXProperty();
	}
	
	public DoubleExpression rightProperty() {
		return this.layoutXProperty().add(this.widthProperty());
	}
	
	public DoubleExpression xProperty() {
		return this.leftProperty();
	}
	
	public DoubleExpression yProperty() {
		return this.layoutYProperty().add(this.heightProperty().multiply(0.5));
	}
	
	
	private void calculateColor(ExpandedFormula data) {
		if (data.getValue() instanceof String) {
			this.setStyle("-fx-background-color: gray; -fx-text-fill: white; -fx-padding: 10px;");
		} else if (!(Boolean)data.getValue()) {
			this.setStyle("-fx-background-color: orangered; -fx-text-fill: black; -fx-padding: 10px;");
		} else {
			this.setStyle("-fx-background-color: lime; -fx-text-fill: black; -fx-padding: 10px;");
		}
	}
	
	@Override
	public String toString() {
		return this.getText();
	}
}
