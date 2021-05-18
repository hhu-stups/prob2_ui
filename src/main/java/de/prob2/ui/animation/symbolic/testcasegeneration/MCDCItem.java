package de.prob2.ui.animation.symbolic.testcasegeneration;

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
		super(TestCaseGenerationType.MCDC, maxDepth);
		
		this.level = level;
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
	public boolean settingsEqual(final TestCaseGenerationItem other) {
		if (!(other instanceof MCDCItem)) {
			return false;
		}
		final MCDCItem o = (MCDCItem)other;
		return super.settingsEqual(o) && this.getLevel() == o.getLevel();
	}
}
