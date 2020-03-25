package de.prob2.ui.visualisation.magiclayout;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import de.prob2.ui.json.JsonManager;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

public class MagicEdgegroup extends MagicComponent {
	public static final JsonDeserializer<MagicEdgegroup> JSON_DESERIALIZER = MagicEdgegroup::new;

	private final IntegerProperty textSize = new SimpleIntegerProperty();

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
	
	private MagicEdgegroup(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) {
		super(json, typeOfT, context);
		
		final JsonObject object = json.getAsJsonObject();
		this.textSize.set(JsonManager.checkDeserialize(context, object, "textSize", Integer.class));
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
	
	@Override
	public boolean equals(Object other) {
		return super.equals(other);
	}
	
	@Override
	public int hashCode() {
		return super.hashCode();
	}
}
