package de.prob2.ui.animation.symbolic.testcasegeneration;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import de.prob.analysis.testcasegeneration.TestCaseGeneratorMCDCSettings;
import de.prob.analysis.testcasegeneration.TestCaseGeneratorSettings;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.verifications.type.BuiltinValidationTaskTypes;
import de.prob2.ui.verifications.type.ValidationTaskType;

public final class MCDCItem extends TestCaseGenerationItem {
	private final int level;

	@JsonCreator
	public MCDCItem(
		@JsonProperty("id") final String id,
		@JsonProperty("maxDepth") final int maxDepth,
		@JsonProperty("level") final int level
	) {
		super(id, maxDepth);
		this.level = level;
	}

	@Override
	public ValidationTaskType<MCDCItem> getTaskType() {
		return BuiltinValidationTaskTypes.TEST_CASE_GENERATION_MCDC;
	}

	@Override
	public String getTaskType(I18n i18n) {
		return i18n.translate("animation.testcase.type.mcdc");
	}

	public int getLevel() {
		return this.level;
	}

	@Override
	public TestCaseGeneratorSettings getTestCaseGeneratorSettings() {
		return new TestCaseGeneratorMCDCSettings(this.getMaxDepth() - 1, this.getLevel());
	}

	@Override
	public String getConfigurationDescription() {
		return "MCDC:" + this.getLevel() + "/" + "DEPTH:" + this.getMaxDepth();
	}

	@Override
	public boolean settingsEqual(Object other) {
		return other instanceof MCDCItem that
			       && super.settingsEqual(that)
			       && Objects.equals(this.getLevel(), that.getLevel());
	}

	@Override
	public MCDCItem copy() {
		return new MCDCItem(this.getId(), this.getMaxDepth(), this.level);
	}
}
