package de.prob2.ui.verifications;

public enum CheckingType {
	REPLAY("verifications.checkingType.replay"),
	LTL("verifications.checkingType.ltl"),
	SYMBOLIC_CHECKING("verifications.checkingType.symbolic.checking"),
	SYMBOLIC_ANIMATION("verifications.checkingType.symbolic.animation"),
	TEST_CASE_GENERATION("verifications.checkingType.testcasegeneration"),
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
