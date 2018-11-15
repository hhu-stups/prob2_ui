package de.prob2.ui.visualisation.magiclayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.paint.Color;

public abstract class MagicComponent {
	
	private final StringProperty name = new SimpleStringProperty();
	private final StringProperty expression = new SimpleStringProperty();
	
	private final ObjectProperty<List<Double>> lineType = new SimpleObjectProperty<>();
	private final ObjectProperty<Color> lineColor = new SimpleObjectProperty<>();
	private final DoubleProperty lineWidth = new SimpleDoubleProperty();
	private final ObjectProperty<Color> textColor = new SimpleObjectProperty<>();
	
	private final boolean editable;
	
	public MagicComponent(String name) {
		this(name, "", true);
	}
	
	public MagicComponent(String name, String expression, boolean editable) {
		this.name.set(name);
		this.expression.set(expression);
		
		this.lineType.set(new ArrayList<>());
		this.lineColor.set(Color.BLACK);
		this.lineWidth.set(1);
		this.textColor.set(Color.BLACK);
		
		this.editable = editable;
	}
	
	public StringProperty nameProperty() {
		return name;
	}
	
	public String getName() {
		return name.get();
	}
	
	public StringProperty expressionProperty() {
		return expression;
	}
	
	public String getExpression() {
		return expression.get();
	}
	
	public ObjectProperty<List<Double>> lineTypeProperty() {
		return lineType;
	}
	
	public List<Double> getLineType() {
		return lineType.get();
	}
	
	public ObjectProperty<Color> lineColorProperty() {
		return lineColor;
	}
	
	public Color getLineColor() {
		return lineColor.get();
	}
	
	public DoubleProperty lineWidthProperty() {
		return lineWidth;
	}
	
	public double getLineWidth() {
		return lineWidth.get();
	}

	public ObjectProperty<Color> textColorProperty() {
		return textColor;
	}

	public Color getTextColor() {
		return textColor.get();
	}
	
	public boolean isEditable() {
		return editable;
	}

	@Override
	public String toString() {
		return name.get();
	}
	
	@Override
	public boolean equals(Object other) {
		if (other == this) {
			return true;
		}
		if (!(other instanceof MagicComponent)) {
			return false;
		}
		MagicComponent otherComponent = (MagicComponent) other;
		return otherComponent.name.get().equals(this.name.get());
	}
	
	@Override
	public int hashCode() {
		 return Objects.hash(name.get());
	}

	public void unbindAll() {
		expression.unbind();
		lineType.unbind();
		lineColor.unbind();
		lineWidth.unbind();
		textColor.unbind();
	}
}
