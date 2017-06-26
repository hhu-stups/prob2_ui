package de.prob2.ui.verifications.ltl.patterns;

import java.util.Objects;

import de.prob2.ui.verifications.ltl.LTLCheckableItem;

public class LTLPatternItem extends LTLCheckableItem {
		
	protected String code;
	
	public LTLPatternItem(String name, String description, String code) {
		super(name, description);
		setCode(code);
	}	
	
	public void setData(String name, String description, String code) {
		initializeStatus();
		setName(name);
		setDescription(description);
		setCode(code);
	}
	
	public void setCode(String code) {
		this.code = code;
	}
	
	public String getCode() {
		return code;
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
