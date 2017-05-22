package de.prob2.ui.verifications.ltl.patterns;

import com.google.inject.Inject;
import com.google.inject.Injector;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import javafx.fxml.FXML;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Modality;

public class LTLPatternDialog extends Dialog<LTLPatternItem> {
	
	@FXML
	private TextField tfName;
	
	@FXML
	private TextArea taDescription;
	
	@FXML
	private TextArea taPattern;
	
	@Inject
	public LTLPatternDialog(final StageManager stageManager, final Injector injector, final CurrentProject currentProject) {
		super();
		this.setResultConverter(type -> {
			if(type == null || type.getButtonData() == ButtonBar.ButtonData.CANCEL_CLOSE) {
				return null;
			} else {
				currentProject.setSaved(false);
				return new LTLPatternItem(tfName.getText(), taDescription.getText(), taPattern.getText());
			}
		});
		this.initModality(Modality.APPLICATION_MODAL);
		stageManager.loadFXML(this, "ltlpattern_dialog.fxml");
	}
		
	public void setData(String name, String description, String formula) {
		tfName.setText(name);
		taDescription.setText(description);
		taPattern.setText(formula);
	}
	
	public void clear() {
		this.tfName.clear();
		this.taDescription.clear();
		this.taPattern.clear();
	}

}
