package de.prob2.ui.verifications;

public enum CheckingType {
	LTL("verifications.checkingType.ltl"),
	SYMBOLIC("verifications.checkingType.symbolic"),
	MODELCHECKING("verifications.checkingType.modelchecking")
	;
	
	private final String key;
	
	CheckingType(final String key) {
		this.key = key;
	}
	
	public String getKey() {
		return this.key;
	}
}
