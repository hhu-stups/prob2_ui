package de.prob2.ui.verifications.ltl;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import de.prob.json.HasMetadata;
import de.prob.json.JsonMetadata;
import de.prob.json.JsonMetadataBuilder;
import de.prob2.ui.verifications.ltl.formula.LTLFormulaItem;
import de.prob2.ui.verifications.ltl.patterns.LTLPatternItem;

import java.util.List;

public class LTLData implements HasMetadata {
	public static final String FILE_TYPE = "LTL";
	public static final int CURRENT_FORMAT_VERSION = 1;

	private List<LTLFormulaItem> formulas;

	private List<LTLPatternItem> patterns;

	private final JsonMetadata metadata;

	@JsonCreator
	public LTLData(
		@JsonProperty("formulas") final List<LTLFormulaItem> formulas,
		@JsonProperty("patterns") final List<LTLPatternItem> patterns,
		@JsonProperty("metadata") final JsonMetadata metadata
	) {
		this.formulas = formulas;
		this.patterns = patterns;
		this.metadata = metadata;
	}

	public static JsonMetadataBuilder metadataBuilder() {
		return new JsonMetadataBuilder(FILE_TYPE, CURRENT_FORMAT_VERSION)
			.withUserCreator()
			.withSavedNow();
	}

	public List<LTLFormulaItem> getFormulas() {
		return formulas;
	}

	public List<LTLPatternItem> getPatterns() {
		return patterns;
	}

	@Override
	public JsonMetadata getMetadata() {
		return this.metadata;
	}

	@Override
	public LTLData withMetadata(final JsonMetadata metadata) {
		return new LTLData(this.getFormulas(), this.getPatterns(), metadata);
	}
}
