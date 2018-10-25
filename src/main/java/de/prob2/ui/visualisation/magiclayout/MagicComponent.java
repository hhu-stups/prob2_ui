package de.prob2.ui.visualisation.magiclayout;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.paint.Color;

public class MagicComponent {
	
	private String name;
	private StringProperty expression = new SimpleStringProperty();
	
	private StringProperty lineType = new SimpleStringProperty();
	private ObjectProperty<Color> lineColor = new SimpleObjectProperty<>();
	private DoubleProperty lineThickness = new SimpleDoubleProperty();
	
	public MagicComponent(String name, String expression) {
		this.name = name;
		this.expression.set(expression);
		
		this.lineType.set("");
		this.lineColor.set(Color.BLACK);
		this.lineThickness.set(1);
	}
	
	public StringProperty expressionProperty() {
		return expression;
	}
	
	public String getExpression() {
		return expression.get();
	}
	
	public StringProperty lineTypeProperty() {
		return lineType;
	}
	
	public String getLineType() {
		return lineType.get();
	}
	
	public ObjectProperty<Color> lineColorProperty() {
		return lineColor;
	}
	
	public Color getLineColor() {
		return lineColor.get();
	}
	
	public DoubleProperty lineThicknessProperty() {
		return lineThickness;
	}
	
	public double getLineThickness() {
		return lineThickness.get();
	}

	@Override
	public String toString() {
		return name;
	}

	public void unbindAll() {
		expression.unbind();
		lineType.unbind();
		lineColor.unbind();
		lineThickness.unbind();
	}
}
