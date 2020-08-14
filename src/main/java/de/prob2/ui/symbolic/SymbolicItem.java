package de.prob2.ui.symbolic;


import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.prob.json.JsonManager;
import de.prob2.ui.verifications.AbstractCheckableItem;

import java.lang.reflect.Type;

public abstract class SymbolicItem extends AbstractCheckableItem {
	
	private final SymbolicExecutionType type;

	public SymbolicItem(String name, String code, SymbolicExecutionType type) {
		super(name, type.getName(), code);
		this.type = type;
	}
	
	public SymbolicItem(String name, SymbolicExecutionType type) {
		super(name, type.getName(), name);
		this.type = type;
	}
	
	protected SymbolicItem(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) {
		super(json, typeOfT, context);
		
		final JsonObject object = json.getAsJsonObject();
		this.type = JsonManager.checkDeserialize(context, object, "type", SymbolicExecutionType.class);
	}
	
	public SymbolicExecutionType getType() {
		return type;
	}
	
	@Override
	public String toString() {
		return String.join(" ", this.getName(), this.getCode(), this.getType().name());
	}
}
