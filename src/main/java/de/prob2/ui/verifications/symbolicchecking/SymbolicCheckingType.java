package de.prob2.ui.verifications.symbolicchecking;

import com.fasterxml.jackson.annotation.JsonProperty;

import de.prob2.ui.internal.Translatable;

public enum SymbolicCheckingType implements Translatable {
	@JsonProperty("INVARIANT")
	CBC_INVARIANT_PRESERVATION_CHECKING("verifications.symbolicchecking.type.invariant"),
	@JsonProperty("DEADLOCK")
	CBC_DEADLOCK_FREEDOM_CHECKING("verifications.symbolicchecking.type.deadlock"),
	@JsonProperty("CHECK_REFINEMENT")
	CBC_REFINEMENT_CHECKING("verifications.symbolicchecking.type.refinementChecking"),
	@JsonProperty("CHECK_STATIC_ASSERTIONS")
	CBC_STATIC_ASSERTION_CHECKING("verifications.symbolicchecking.type.staticAssertionChecking"),
	@JsonProperty("CHECK_DYNAMIC_ASSERTIONS")
	CBC_DYNAMIC_ASSERTION_CHECKING("verifications.symbolicchecking.type.dynamicAssertionChecking"),
	@JsonProperty("CHECK_WELL_DEFINEDNESS")
	WELL_DEFINEDNESS_CHECKING("verifications.symbolicchecking.type.wellDefinednessChecking"),
	@JsonProperty("FIND_REDUNDANT_INVARIANTS")
	CBC_FIND_REDUNDANT_INVARIANTS("verifications.symbolicchecking.type.findRedundantInvariants"),
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
