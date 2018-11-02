package de.prob2.ui.visualisation.magiclayout;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.paint.Color;

public class MagicNode extends MagicComponent {
	
	private final BooleanProperty cluster = new SimpleBooleanProperty();
	private final ObjectProperty<MagicShape> shape = new SimpleObjectProperty<>();
	private final ObjectProperty<Color> nodeColor = new SimpleObjectProperty<>();
	
	public MagicNode(String name, String expression) {
		super(name, expression);
		
		this.cluster.set(false);
		this.shape.set(MagicShape.RECTANGLE);
		this.nodeColor.set(Color.WHITE);
	}
	
	public MagicNode(String name) {
		super(name);
		
		this.cluster.set(false);
		this.shape.set(MagicShape.RECTANGLE);
		this.nodeColor.set(Color.WHITE);
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
