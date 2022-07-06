package de.prob2.ui.animation.tracereplay.refactoring;

import com.google.inject.Injector;

import de.prob.check.tracereplay.check.ui.MappingFactoryInterface;
import de.prob.check.tracereplay.check.ui.ToManyOptionsIdentifierMapping;
import de.prob2.ui.animation.tracereplay.TooManyOptionsRequestIdentifierMapping;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;

public class MappingFactory implements MappingFactoryInterface {

	Injector injector;
	StageManager stageManager;

	public MappingFactory(Injector injector, StageManager stageManager) {
		this.injector = injector;
		this.stageManager = stageManager;
	}

	@Override
	public ToManyOptionsIdentifierMapping produceMappingManager() {
		return new TooManyOptionsRequestIdentifierMapping(stageManager, injector.getInstance(I18n.class));
	}
}
