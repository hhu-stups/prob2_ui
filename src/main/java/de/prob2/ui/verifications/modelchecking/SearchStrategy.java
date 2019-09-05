package de.prob2.ui.verifications.modelchecking;

import de.prob.check.ModelCheckingOptions;

public enum SearchStrategy {
	MIXED_BF_DF("verifications.modelchecking.modelcheckingStage.strategy.mixedBfDf"),
	BREADTH_FIRST("verifications.modelchecking.modelcheckingStage.strategy.breadthFirst"),
	DEPTH_FIRST("verifications.modelchecking.modelcheckingStage.strategy.depthFirst"),
	;
	
	private final String name;
	
	SearchStrategy(final String name) {
		this.name = name;
	}
	
	public static SearchStrategy fromOptions(final ModelCheckingOptions options) {
		if (options.getPrologOptions().contains(ModelCheckingOptions.Options.BREADTH_FIRST_SEARCH)) {
			return BREADTH_FIRST;
		} else if (options.getPrologOptions().contains(ModelCheckingOptions.Options.DEPTH_FIRST_SEARCH)) {
			return DEPTH_FIRST;
		} else {
			return MIXED_BF_DF;
		}
	}
	
	public String getName() {
		return this.name;
	}
}
