package de.prob2.ui.visualisation.magiclayout;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.paint.Color;

@JsonPropertyOrder({
	"cluster",
	"shape",
	"nodeColor",
	"name",
	"expression",
	"lineType",
	"lineColor",
	"lineWidth",
	"textColor",
})
public class MagicNodegroup extends MagicComponent {
	private final BooleanProperty cluster = new SimpleBooleanProperty();
	private final ObjectProperty<MagicShape> shape = new SimpleObjectProperty<>();
	private final ObjectProperty<Color> nodeColor = new SimpleObjectProperty<>();
	
	@JsonCreator
	public MagicNodegroup(
		@JsonProperty("name") final String name,
		@JsonProperty("expression") final String expression,
		@JsonProperty("lineType") final MagicLineType lineType,
		@JsonProperty("lineColor") final Color lineColor,
		@JsonProperty("lineWidth") final MagicLineWidth lineWidth,
		@JsonProperty("textColor") final Color textColor,
		@JsonProperty("cluster") final boolean cluster,
		@JsonProperty("shape") final MagicShape shape,
		@JsonProperty("nodeColor") final Color nodeColor
	) {
		super(name, expression, lineType, lineColor, lineWidth, textColor);
		
		this.cluster.set(cluster);
		this.shape.set(shape);
		this.nodeColor.set(nodeColor);
	}
	
	public MagicNodegroup(String name, String expression, boolean cluster) {
		super(name, expression);
		
		this.cluster.set(cluster);
		this.shape.set(MagicShape.RECTANGLE);
		this.nodeColor.set(Color.WHITE);
	}
	
	public MagicNodegroup(String name) {
		this(name, "", false);
	}
	
	public MagicNodegroup(MagicNodegroup nodes) {
		super(nodes);
		
		this.cluster.set(nodes.isCluster());
		this.shape.set(nodes.getShape());
		this.nodeColor.set(nodes.getNodeColor());
	}
	
	public BooleanProperty clusterProperty() {
		return cluster;
	}
	
	public Boolean isCluster() {
		return cluster.get();
	}
	
	public ObjectProperty<MagicShape> shapeProperty() {
		return shape;
	}
	
	public MagicShape getShape() {
		return shape.get();
	}
	
	public ObjectProperty<Color> nodeColorProperty() {
		return nodeColor;
	}
	
	public Color getNodeColor() {
		return nodeColor.get();
	}

	@Override
	public void unbindAll() {
		super.unbindAll();
		
		cluster.unbind();
		shape.unbind();
		nodeColor.unbind();
	}
}
