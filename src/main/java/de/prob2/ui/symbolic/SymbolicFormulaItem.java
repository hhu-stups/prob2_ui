package de.prob2.ui.symbolic;


import de.prob2.ui.verifications.AbstractCheckableItem;

public abstract class SymbolicFormulaItem extends AbstractCheckableItem {
	
	protected SymbolicExecutionType type;

	public SymbolicFormulaItem(String name, String code, SymbolicExecutionType type) {
		super(name, type.getName(), code);
		this.type = type;
	}
	
	public SymbolicFormulaItem(String name, SymbolicExecutionType type) {
		super(name, type.getName(), name);
		this.type = type;
	}
	
	public void reset() {
		this.initialize();
	}
	
	public void setType(SymbolicExecutionType type) {
		this.type = type;
	}
	
	public SymbolicExecutionType getType() {
		return type;
	}
	
	@Override
	public String toString() {
		return String.join(" ", name, code, type.name());
	}
	
	
	public void setData(String name, String description, String code, SymbolicExecutionType type) {
		super.setData(name, description, code);
		this.type = type;
	}
	

}
