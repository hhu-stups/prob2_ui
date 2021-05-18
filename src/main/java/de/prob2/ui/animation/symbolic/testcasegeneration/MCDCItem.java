package de.prob2.ui.animation.symbolic.testcasegeneration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

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
	public String getConfigurationDescription() {
		return "MCDC:" + this.getLevel() + "/" + "DEPTH:" + this.getMaxDepth();
	}
}
