package de.prob2.ui.verifications.modelchecking;

public enum SearchStrategy {
	MIXED_BF_DF("verifications.modelchecking.modelcheckingStage.strategy.mixedBfDf"),
	BREADTH_FIRST("verifications.modelchecking.modelcheckingStage.strategy.breadthFirst"),
	DEPTH_FIRST("verifications.modelchecking.modelcheckingStage.strategy.depthFirst"),
	;
	
	private final String name;
	
	SearchStrategy(final String name) {
		this.name = name;
	}
	
	public String getName() {
		return this.name;
	}
}
