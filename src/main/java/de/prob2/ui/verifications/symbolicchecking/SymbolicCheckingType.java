package de.prob2.ui.verifications.symbolicchecking;

import de.prob2.ui.symbolic.SymbolicExecutionType;

public enum SymbolicCheckingType implements SymbolicExecutionType {

	INVARIANT("verifications.symbolicchecking.type.invariant"),
	DEADLOCK("verifications.symbolicchecking.type.deadlock"),
	CHECK_REFINEMENT("verifications.symbolicchecking.type.refinementChecking"),
	CHECK_STATIC_ASSERTIONS("verifications.symbolicchecking.type.staticAssertionChecking"),
	CHECK_DYNAMIC_ASSERTIONS("verifications.symbolicchecking.type.dynamicAssertionChecking"),
	CHECK_WELL_DEFINEDNESS("verifications.symbolicchecking.type.wellDefinednessChecking"),
	FIND_REDUNDANT_INVARIANTS("verifications.symbolicchecking.type.findRedundantInvariants"),
	SYMBOLIC_MODEL_CHECK("verifications.symbolicchecking.type.symbolicModelChecking"),
	;

	private final String translationKey;

	SymbolicCheckingType(final String translationKey) {
		this.translationKey = translationKey;
	}

	@Override
	public String getTranslationKey() {
		return this.translationKey;
	}
}
