package de.prob2.ui.visualisation.magiclayout;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.paint.Color;

@JsonPropertyOrder({
	"textSize",
	"name",
	"expression",
	"lineType",
	"lineColor",
	"lineWidth",
	"textColor",
})
public class MagicEdgegroup extends MagicComponent {
	private final IntegerProperty textSize = new SimpleIntegerProperty();

	@JsonCreator
	public MagicEdgegroup(
		@JsonProperty("name") final String name,
		@JsonProperty("expression") final String expression,
		@JsonProperty("lineType") final MagicLineType lineType,
		@JsonProperty("lineColor") final Color lineColor,
		@JsonProperty("lineWidth") final MagicLineWidth lineWidth,
		@JsonProperty("textColor") final Color textColor,
		@JsonProperty("textSize") final int textSize
	) {
		super(name, expression, lineType, lineColor, lineWidth, textColor);

		this.textSize.set(textSize);
	}

	public MagicEdgegroup(String name, String expression) {
		super(name, expression);

		this.textSize.set(12);
	}

	public MagicEdgegroup(String name) {
		this(name, "");
	}
	
	public MagicEdgegroup(MagicEdgegroup edges) {
		super(edges);
		
		this.textSize.set(edges.getTextSize());
	}

	public IntegerProperty textSizeProperty() {
		return textSize;
	}

	public int getTextSize() {
		return textSize.get();
	}

	@Override
	public void unbindAll() {
		super.unbindAll();

		textSize.unbind();
	}
}
