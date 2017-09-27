package de.prob2.ui.verifications;

public enum CheckingType {
	LTL("verifications.checkingType.ltl"),
	CBC("verifications.checkingType.cbc"),
	;
	
	private final String key;
	
	private CheckingType(final String key) {
		this.key = key;
	}
	
	public String getKey() {
		return this.key;
	}
}
