package de.prob2.ui.verifications.ltl.patterns;

import java.util.Objects;

import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.verifications.ltl.LTLCheckableItem;

public class LTLPatternItem extends LTLCheckableItem {
		
	private String code;
	
	public LTLPatternItem(String name, String description, String code) {
		super(name, description);
		initializeStatus();
		setData(name, description, code);
		
	}	
	
	public void setData(String name, String description, String code) {
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
		if (!(obj instanceof Machine)) {
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
