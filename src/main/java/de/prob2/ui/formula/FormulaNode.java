package de.prob2.ui.formula;

import java.util.ArrayList;
import java.util.List;

import de.prob.animator.domainobjects.ExpandedFormula;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Ellipse;
import javafx.scene.text.Text;


public class FormulaNode extends Region {
		
	private Ellipse ellipse;
	private Text text;
	private Color color;
	public List<FormulaNode> next;
	
	public FormulaNode(ExpandedFormula data) {
		next = new ArrayList<FormulaNode>();
		text = new Text(data.getLabel());
		double width = text.getLayoutBounds().getWidth();
		double height = text.getLayoutBounds().getHeight();
		ellipse = new Ellipse(width, height);
		color = calculateColor(data);
		if(data.getChildren() == null || data.getChildren().isEmpty()) {
			return;
		}
		for(int i = 0; i < data.getChildren().size(); i++) {
			next.add(new FormulaNode(data.getChildren().get(i)));
		}
		
	}
		
	public FormulaNode(double centerX, double centerY, ExpandedFormula data) {
		this(data);
		setPosition(centerX, centerY);
	}
	

	
	public void setPosition(double x, double y) {
		double width = text.getLayoutBounds().getWidth();
		double height = text.getLayoutBounds().getHeight();
		text.setX(x);
		text.setY(y + height/2);
		ellipse.setCenterX(x + width/2);
		ellipse.setCenterY(y);
		draw();
	}
	
	public double getLeft() {
		return ellipse.getCenterX() - ellipse.getRadiusX();
	}
	
	public double getRight() {
		return ellipse.getCenterX() + ellipse.getRadiusX();
	}
	
	public double getX() {
		return ellipse.getCenterX();
	}

	public double getY() {
		return ellipse.getCenterY();
	}
	
	
	private void draw() {
		text.setFill(Color.BLACK);
		ellipse.setStroke(Color.BLACK);
		setFill(color);
		this.getChildren().add(ellipse);
		this.getChildren().add(text);
	}
	
	private Color calculateColor(ExpandedFormula data) {
		if(data.getValue() instanceof Boolean && (Boolean) data.getValue() == false) {
			return Color.ORANGERED;
		}
		return Color.LIME;
	}
	
	
	private void setFill(Paint value) {
		ellipse.setFill(value);
	}
	
	public String toString() {
		return text.getText();
	}
	

}
