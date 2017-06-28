package de.prob2.ui.verifications.ltl.formula;

import com.google.inject.Inject;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.verifications.ltl.LTLDialog;

public class LTLFormulaDialog extends LTLDialog<LTLFormulaItem> {
			
	@Inject
	public LTLFormulaDialog(final StageManager stageManager) {
		super();
		stageManager.loadFXML(this, "ltlformula_dialog.fxml");
	}
	
}
