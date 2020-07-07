package de.prob2.ui.visualisation.magiclayout;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.prob.json.JsonManager;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.paint.Color;

import java.lang.reflect.Type;

public class MagicNodegroup extends MagicComponent {
	public static final JsonDeserializer<MagicNodegroup> JSON_DESERIALIZER = MagicNodegroup::new;
	
	private final BooleanProperty cluster = new SimpleBooleanProperty();
	private final ObjectProperty<MagicShape> shape = new SimpleObjectProperty<>();
	private final ObjectProperty<Color> nodeColor = new SimpleObjectProperty<>();
	
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
	
	private MagicNodegroup(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) {
		super(json, typeOfT, context);
		
		final JsonObject object = json.getAsJsonObject();
		this.cluster.set(JsonManager.checkDeserialize(context, object, "cluster", Boolean.class));
		this.shape.set(JsonManager.checkDeserialize(context, object, "shape", MagicShape.class));
		this.nodeColor.set(JsonManager.checkDeserialize(context, object, "nodeColor", Color.class));
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
	
	@Override
	public boolean equals(Object other) {
		return super.equals(other);
	}
	
	@Override
	public int hashCode() {
		return super.hashCode();
	}
}
