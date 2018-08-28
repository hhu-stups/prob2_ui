package de.prob2.ui.verifications.symbolicchecking;

import javax.inject.Inject;

import com.google.inject.Singleton;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.symbolic.SymbolicChoosingStage;

@Singleton
public class SymbolicCheckingChoosingStage extends SymbolicChoosingStage {
	
	@Inject
	private SymbolicCheckingChoosingStage(final StageManager stageManager) {
		super();
		stageManager.loadFXML(this, "symbolic_checking_choice.fxml");
	}
	
}
