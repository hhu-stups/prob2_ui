package de.prob2.ui.modelchecking.ltl;

import com.google.inject.Inject;

import de.prob2.ui.internal.StageManager;
import javafx.scene.control.Dialog;
import javafx.stage.Modality;

public class AddLTLFormulaDialog extends Dialog {

	@Inject
	public AddLTLFormulaDialog(final StageManager stageManager) {
		super();
		this.initModality(Modality.APPLICATION_MODAL);
		stageManager.loadFXML(this, "ltlformula_dialog.fxml");
	}
	
}
