package de.prob2.ui.verifications.ltl;

import com.google.inject.Inject;
import com.google.inject.Injector;

import de.prob2.ui.internal.StageManager;
import javafx.fxml.FXML;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Modality;

public class AddLTLFormulaDialog extends Dialog<LTLFormulaItem> {
	
	@FXML
	private TextField tfName;
	
	@FXML
	private TextArea taDescription;

	@Inject
	public AddLTLFormulaDialog(final StageManager stageManager, final Injector injector) {
		super();
		this.setResultConverter(type -> {
			if(type == null || type.getButtonData() == ButtonBar.ButtonData.CANCEL_CLOSE) {
				return null;
			} else {
				LTLFormulaStage formulaStage = injector.getInstance(LTLFormulaStage.class);
				formulaStage.setTitle(tfName.getText());
				LTLFormulaItem item = new LTLFormulaItem(tfName.getText(), taDescription.getText());
				item.setFormulaStage(formulaStage);
				return item;
			}
		});
		this.initModality(Modality.APPLICATION_MODAL);
		stageManager.loadFXML(this, "ltlformula_dialog.fxml");
	}
	
	public void setName(String name) {
		tfName.setText(name);
	}
	
	public void setDescription(String description) {
		taDescription.setText(description);
	}
	
}
