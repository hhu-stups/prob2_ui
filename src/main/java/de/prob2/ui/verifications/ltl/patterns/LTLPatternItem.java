package de.prob2.ui.verifications.ltl.patterns;

import java.lang.reflect.Type;
import java.util.Objects;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;

import de.prob2.ui.verifications.AbstractCheckableItem;
import de.prob2.ui.verifications.ltl.ILTLItem;

public class LTLPatternItem extends AbstractCheckableItem implements ILTLItem {
	public static final JsonDeserializer<LTLPatternItem> JSON_DESERIALIZER = LTLPatternItem::new;
	
	public LTLPatternItem(String code, String description) {
		super("", description, code);
	}	
		
	private LTLPatternItem(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) {
		super(json, typeOfT, context);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof LTLPatternItem)) {
			return false;
		}
		LTLPatternItem otherItem = (LTLPatternItem) obj;
		return otherItem.getName().equals(this.getName());
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(this.getName());
	}
		
}
