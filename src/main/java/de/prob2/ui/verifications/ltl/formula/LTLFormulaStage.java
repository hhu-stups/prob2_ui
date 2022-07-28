package de.prob2.ui.verifications.ltl.formula;

import com.google.inject.Inject;

import de.prob.exception.ProBError;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.layout.FontSize;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.verifications.ltl.LTLItemStage;
import de.prob2.ui.verifications.ltl.LTLResultHandler;
import de.prob2.ui.verifications.ltl.patterns.builtins.LTLBuiltinsStage;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;

public class LTLFormulaStage extends LTLItemStage<LTLFormulaItem> {
	@FXML
	private TextField idTextField;

	@FXML
	private Button applyButton;
	
	private final LTLFormulaChecker formulaChecker;

	private LTLFormulaItem result;
	
	@Inject
	public LTLFormulaStage(
		final StageManager stageManager, final CurrentProject currentProject, final FontSize fontSize,
		final LTLFormulaChecker formulaChecker, final LTLResultHandler resultHandler, final LTLBuiltinsStage builtinsStage
	) {
		super(currentProject, fontSize, resultHandler, builtinsStage);
		this.formulaChecker = formulaChecker;
		this.result = null;
		stageManager.loadFXML(this, "ltlformula_stage.fxml");
	}

	public void setData(final LTLFormulaItem item) {
		idTextField.setText(item.getId() == null ? "" : item.getId());
		taCode.replaceText(item.getCode());
		taDescription.setText(item.getDescription());
	}

	@FXML
	private void applyFormula() {
		result = null;
		final String id = idTextField.getText().trim().isEmpty() ? null : idTextField.getText();
		String code = taCode.getText();
		final LTLFormulaItem item = new LTLFormulaItem(id, code, taDescription.getText());
		try {
			formulaChecker.parseFormula(item.getCode(), currentProject.getCurrentMachine());
		} catch (ProBError e) {
			this.showErrors(e.getErrors());
			return;
		}
		result = item;
		this.close();
	}
	
	@FXML
	private void cancel() {
		this.result = null;
		this.close();
	}

	public LTLFormulaItem getResult() {
		return result;
	}
}
