package de.prob2.ui.animation.tracereplay;

import com.google.inject.Injector;
import com.google.inject.Stage;
import de.prob.check.tracereplay.check.exceptions.MappingFactoryInterface;
import de.prob.check.tracereplay.check.exceptions.ToManyOptionsIdentifierMapping;
import de.prob2.ui.internal.StageManager;

public class MappingFactory implements MappingFactoryInterface {


	Injector injector;
	StageManager stageManager;
	public MappingFactory(Injector injector, StageManager stageManager){
		this.injector = injector;
		this.stageManager = stageManager;
	}

	@Override
	public ToManyOptionsIdentifierMapping produceMappingManager() {
		return new ToManyOptionsRequestIdentifierMapping(injector, stageManager);
	}
}
