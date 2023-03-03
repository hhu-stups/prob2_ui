package de.prob2.ui.verifications.temporal.ltl;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import de.prob.json.HasMetadata;
import de.prob.json.JsonMetadata;
import de.prob.json.JsonMetadataBuilder;
import de.prob2.ui.verifications.temporal.ltl.patterns.LTLPatternItem;
import de.prob2.ui.verifications.temporal.TemporalFormulaItem;

import java.util.List;

public class LTLData implements HasMetadata {
	public static final String FILE_TYPE = "LTL";
	public static final int CURRENT_FORMAT_VERSION = 1;

	private List<TemporalFormulaItem> formulas;

	private List<LTLPatternItem> patterns;

	private final JsonMetadata metadata;

	@JsonCreator
	public LTLData(
		@JsonProperty("formulas") final List<TemporalFormulaItem> formulas,
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

	public List<TemporalFormulaItem> getFormulas() {
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
