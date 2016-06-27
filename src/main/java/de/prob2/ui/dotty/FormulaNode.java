package de.prob2.ui.dotty;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Ellipse;
import javafx.scene.text.Text;


public class FormulaNode extends Region {
	
	private Ellipse ellipse;
	private Text text;
	public List<FormulaNode> next;
	//public FormulaNode next;
	
	public FormulaNode(double centerX, double centerY, String data, List<FormulaNode> list) {
		//has to be changed a little
		text = new Text(centerX, centerY, data);
		double width = text.getLayoutBounds().getWidth();
		double height = text.getLayoutBounds().getHeight();
		text.setX(text.getX() - width/2);
		text.setY(text.getY() + height/2);
		ellipse = new Ellipse(centerX, centerY, width, height);
		next = list;
		//next = null;
		show();
	}
	
	public FormulaNode(String data, List<FormulaNode> list) {
		
		text = new Text(data);
		double width = text.getLayoutBounds().getWidth();
		double height = text.getLayoutBounds().getHeight();
		ellipse = new Ellipse(width, height);
		next = list;
		//next = null;
	}
	
	public void setPosition(double x, double y) {
		//Can be improved
		double width = text.getLayoutBounds().getWidth();
		double height = text.getLayoutBounds().getHeight();
		text.setX(x);
		text.setY(y + height/2);
		ellipse.setCenterX(x + width/2);
		ellipse.setCenterY(y);
		show();
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
	
	
	public void show() {
		text.setFill(Color.BLACK);
		ellipse.setStroke(Color.BLACK);
		setFill(Color.WHITE);
		this.getChildren().add(ellipse);
		this.getChildren().add(text);
	}


	public void setFill(Paint value) {
		ellipse.setFill(value);
	}
	
	public String toString() {
		return text.getText();
	}

}
