package de.prob2.ui.verifications.ltl;

public abstract class LTLAbstractItem extends LTLCheckableItem {
	
	protected String code;
	
	public LTLAbstractItem(String name, String description, String code) {
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

}
