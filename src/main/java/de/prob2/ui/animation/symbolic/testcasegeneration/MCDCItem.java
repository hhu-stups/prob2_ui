package de.prob2.ui.animation.symbolic.testcasegeneration;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import de.prob.analysis.testcasegeneration.TestCaseGeneratorMCDCSettings;
import de.prob.analysis.testcasegeneration.TestCaseGeneratorSettings;

public final class MCDCItem extends TestCaseGenerationItem {
	private final int level;

	@JsonCreator
	public MCDCItem(
		@JsonProperty("maxDepth") final int maxDepth,
		@JsonProperty("level") final int level
	) {
		super(maxDepth);
		this.level = level;
	}

	@Override
	public TestCaseGenerationType getType() {
		return TestCaseGenerationType.MCDC;
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
}
