package de.prob2.ui.verifications.cbc;

import java.util.Objects;

import de.prob2.ui.verifications.AbstractCheckableItem;

public class CBCFormulaItem extends AbstractCheckableItem {

	public CBCFormulaItem(String name, String description, String code) {
		super(name, description, code);
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
		return otherItem.getName().equals(this.getName());
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(name, description, code);
	}

}
