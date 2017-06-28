package de.prob2.ui.verifications.ltl.patterns;

import java.util.Objects;

import de.prob2.ui.verifications.ltl.LTLAbstractItem;

public class LTLPatternItem extends LTLAbstractItem {
	
	public LTLPatternItem(String name, String description, String code) {
		super(name, description, code);
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
		return Objects.hash(name, description, code);
	}
		
}
