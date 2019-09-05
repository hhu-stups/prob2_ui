package de.prob2.ui.verifications.modelchecking;

import de.prob.check.ModelCheckingOptions;

public enum SearchStrategy {
	MIXED_BF_DF("verifications.modelchecking.modelcheckingStage.strategy.mixedBfDf", o -> o.breadthFirst(false).depthFirst(false)),
	BREADTH_FIRST("verifications.modelchecking.modelcheckingStage.strategy.breadthFirst", o -> o.breadthFirst(true).depthFirst(false)),
	DEPTH_FIRST("verifications.modelchecking.modelcheckingStage.strategy.depthFirst", o -> o.breadthFirst(false).depthFirst(true)),
	;
	
	@FunctionalInterface
	private interface OptionsApplier {
		ModelCheckingOptions apply(final ModelCheckingOptions options);
	}
	
	private final String name;
	private final OptionsApplier optionsApplier;
	
	SearchStrategy(final String name, final OptionsApplier optionsApplier) {
		this.name = name;
		this.optionsApplier = optionsApplier;
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
	
	public ModelCheckingOptions toOptions(final ModelCheckingOptions options) {
		return this.optionsApplier.apply(options);
	}
}
