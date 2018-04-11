package de.prob2.ui.formula;

import de.prob.animator.domainobjects.ExpandedFormula;
import de.prob2.ui.layout.FontSize;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;

import java.util.ArrayList;
import java.util.List;


public class FormulaNode extends Region {
		
	private Rectangle rectangle;
	private Text text;
	private Color color;
	List<FormulaNode> next;
	private double width;
	private double height;
	private final FontSize fontSize;
	
	public FormulaNode(ExpandedFormula data, FontSize fontSize) {
		this.fontSize = fontSize;
		next = new ArrayList<>();
		text = new Text(data.getLabel());
		if (data.getValue() instanceof String) {
			text.setText(text.getText() + " = " + data.getValue());
		}
		width = text.getLayoutBounds().getWidth();
		height = text.getLayoutBounds().getHeight();
		rectangle = new Rectangle(width * fontSize.getFontSize()/FontSize.DEFAULT_FONT_SIZE, height * 2 * fontSize.getFontSize()/FontSize.DEFAULT_FONT_SIZE);
		color = calculateColor(data);
		if (data.getChildren() == null || data.getChildren().isEmpty()) {
			return;
		}
		for (int i = 0; i < data.getChildren().size(); i++) {
			next.add(new FormulaNode(data.getChildren().get(i), fontSize));
		}
	}
		
	
	public double getNodeWidth() {
		return width;
	}
	
	public double getNodeHeight() {
		return height;
	}
	
	
	public void setPosition(double x, double y) {
		text.setX(x);
		text.setY(y + height * fontSize.getFontSize()/FontSize.DEFAULT_FONT_SIZE/2);
		rectangle.setX(x);
		rectangle.setY(y - height * fontSize.getFontSize()/FontSize.DEFAULT_FONT_SIZE);
		draw();
	}
	
	public double getLeft() {
		return rectangle.getX();
	}
	
	public double getRight() {
		return rectangle.getX() + rectangle.getWidth();
	}
	
	public double getX() {
		return getLeft();
	}

	public double getY() {
		return rectangle.getY() + 0.5 * rectangle.getHeight();
	}
	
	
	private void draw() {
		text.setFill(Color.BLACK);
		if (color.equals(Color.GRAY)) {
			text.setFill(Color.WHITE);
		}
		rectangle.setStroke(Color.BLACK);
		setFill(color);
		this.getChildren().add(rectangle);
		this.getChildren().add(text);
	}
	
	private Color calculateColor(ExpandedFormula data) {
		if (data.getValue() instanceof String) {
			return Color.GRAY;
		} else if (!(Boolean)data.getValue()) {
			return Color.ORANGERED;
		} else {
			return Color.LIME;
		}
	}
	
	
	private void setFill(Paint value) {
		rectangle.setFill(value);
	}
	
	@Override
	public String toString() {
		return text.getText();
	}
}
