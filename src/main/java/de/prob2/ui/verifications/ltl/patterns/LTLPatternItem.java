package de.prob2.ui.verifications.ltl.patterns;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;

import de.prob2.ui.verifications.AbstractCheckableItem;
import de.prob2.ui.verifications.ltl.ILTLItem;

public class LTLPatternItem extends AbstractCheckableItem implements ILTLItem {
	public static final JsonDeserializer<LTLPatternItem> JSON_DESERIALIZER = LTLPatternItem::new;
	
	public LTLPatternItem(final String name, final String description, final String code) {
		super(name, description, code);
	}
	
	private LTLPatternItem(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) {
		super(json, typeOfT, context);
	}
	
	public boolean settingsEqual(final LTLPatternItem other) {
		return this.getName().equals(other.getName());
	}
}
