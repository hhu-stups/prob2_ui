package de.prob2.ui.verifications.cbc;

import java.util.Objects;

import de.prob.statespace.Trace;

public class CBCFormulaFindStateItem extends CBCFormulaItem {
	
	private Trace example;
	
	public CBCFormulaFindStateItem(String name, String code, CBCType type) {
		super(name, code, type);
		this.example = null;
	}
	
	public void setExample(Trace example) {
		this.example = example;
	}
	
	public Trace getExample() {
		return example;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof CBCFormulaFindStateItem)) {
			return false;
		}
		CBCFormulaFindStateItem otherItem = (CBCFormulaFindStateItem) obj;
		return otherItem.getName().equals(this.getName()) &&
				otherItem.getCode().equals(this.getCode()) &&
				otherItem.getType().equals(this.getType());
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(name, code, type);
	}

}
