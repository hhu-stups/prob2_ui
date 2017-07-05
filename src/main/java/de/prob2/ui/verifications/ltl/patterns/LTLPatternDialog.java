package de.prob2.ui.verifications.ltl.patterns;

import com.google.inject.Inject;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.verifications.ltl.LTLDialog;

public class LTLPatternDialog extends LTLDialog {
		
	@Inject
	public LTLPatternDialog(final StageManager stageManager) {
		super();
		stageManager.loadFXML(this, "ltlpattern_dialog.fxml");
	}
	
}
