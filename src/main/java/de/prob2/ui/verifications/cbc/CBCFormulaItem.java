package de.prob2.ui.verifications.cbc;

import de.prob2.ui.verifications.AbstractCheckableItem;

import java.util.Objects;

public class CBCFormulaItem extends AbstractCheckableItem {
	
	public enum CBCType {
		INVARIANT,SEQUENCE,DEADLOCK
	}
	
	private CBCType type;

	public CBCFormulaItem(String name, String code, CBCType type) {
		super(name, type.name(), code);
		this.type = type;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof CBCFormulaItem)) {
			return false;
		}
		CBCFormulaItem otherItem = (CBCFormulaItem) obj;
		return otherItem.getName().equals(this.getName()) &&
				otherItem.getType().equals(this.getType());
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(name, type);
	}
	
	public CBCType getType() {
		return type;
	}
	

}
