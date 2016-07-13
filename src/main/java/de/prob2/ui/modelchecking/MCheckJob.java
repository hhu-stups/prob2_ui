package de.prob2.ui.modelchecking;

import de.prob.check.ModelChecker;
import de.prob.check.ModelCheckingOptions;

public class MCheckJob {

	private ModelChecker checker;
	private ModelCheckingOptions options;

	public MCheckJob(ModelChecker checker, ModelCheckingOptions options) {
		this.checker = checker;
		this.options = options;
	}
	
	public ModelChecker getChecker() {
		return checker;
	}
	
	public ModelCheckingOptions getOptions() {
		return options;
	}

}
