package de.prob2.ui.verifications.symbolicchecking;

import com.fasterxml.jackson.annotation.JsonProperty;

import de.prob2.ui.internal.Translatable;

public enum SymbolicCheckingType implements Translatable {
	@JsonProperty("INVARIANT")
	SYMBOLIC_INVARIANT("verifications.symbolicchecking.type.invariant"),
	@JsonProperty("DEADLOCK")
	SYMBOLIC_DEADLOCK("verifications.symbolicchecking.type.deadlock"),
	CHECK_REFINEMENT("verifications.symbolicchecking.type.refinementChecking"),
	CHECK_STATIC_ASSERTIONS("verifications.symbolicchecking.type.staticAssertionChecking"),
	CHECK_DYNAMIC_ASSERTIONS("verifications.symbolicchecking.type.dynamicAssertionChecking"),
	CHECK_WELL_DEFINEDNESS("verifications.symbolicchecking.type.wellDefinednessChecking"),
	FIND_REDUNDANT_INVARIANTS("verifications.symbolicchecking.type.findRedundantInvariants"),
	@JsonProperty("SYMBOLIC_MODEL_CHECK")
	SYMBOLIC_MODEL_CHECKING("verifications.symbolicchecking.type.symbolicModelChecking"),
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
