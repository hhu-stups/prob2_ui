package de.prob2.ui.verifications.ltl.patterns;

import de.prob2.ui.verifications.AbstractCheckableItem;
import de.prob2.ui.verifications.ltl.ILTLItem;

import java.util.Objects;

public class LTLPatternItem extends AbstractCheckableItem implements ILTLItem {
	
	public LTLPatternItem(String code, String description) {
		super("", description, code);
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
		return Objects.hash(name);
	}
		
}
