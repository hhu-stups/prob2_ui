package de.prob2.ui.verifications.ltl.formula;

import com.google.inject.Inject;
import de.prob2.ui.internal.AbstractResultHandler;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.layout.FontSize;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.verifications.ltl.LTLHandleItem;
import de.prob2.ui.verifications.ltl.LTLHandleItem.HandleType;
import de.prob2.ui.verifications.ltl.LTLItemStage;
import de.prob2.ui.verifications.ltl.LTLResultHandler;
import de.prob2.ui.verifications.ltl.patterns.builtins.LTLBuiltinsStage;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import netscape.javascript.JSObject;

public class LTLFormulaStage extends LTLItemStage<LTLFormulaItem> {

	@FXML
	private Button applyButton;
	
	private final LTLFormulaChecker formulaChecker;
	
	@Inject
	public LTLFormulaStage(
		final StageManager stageManager, final CurrentProject currentProject, final FontSize fontSize,
		final LTLFormulaChecker formulaChecker, final LTLResultHandler resultHandler, final LTLBuiltinsStage builtinsStage
	) {
		super(currentProject, fontSize, resultHandler, builtinsStage);
		this.formulaChecker = formulaChecker;
		stageManager.loadFXML(this, "ltlformula_stage.fxml");
	}

	@FXML
	public void initialize() {
		super.initialize();
		applyButton.disableProperty().bind(formulaChecker.runningProperty());
	}

	@FXML
	private void applyFormula() {
		final JSObject editor = (JSObject) engine.executeScript("LtlEditor.cm");
		String code = editor.call("getValue").toString();
		if(handleItem.getHandleType() == HandleType.ADD) {
			addItem(currentProject.getCurrentMachine(), new LTLFormulaItem(code, taDescription.getText()));
		} else {
			changeItem(handleItem.getItem(), new LTLFormulaItem(code, taDescription.getText()));
		}
	}
	
	private void addItem(Machine machine, LTLFormulaItem item) {
		if(machine.getLTLFormulas().stream().noneMatch(item::settingsEqual)) {
			machine.getLTLFormulas().add(item);
			setHandleItem(new LTLHandleItem<>(HandleType.CHANGE, item));
			formulaChecker.checkFormula(item, this);
		} else {
			this.close();
			resultHandler.showAlreadyExists(AbstractResultHandler.ItemType.FORMULA);
		}
	}
	
	private void changeItem(LTLFormulaItem item, LTLFormulaItem result) {
		Machine machine = currentProject.getCurrentMachine();
		if(machine.getLTLFormulas().stream().noneMatch(existing -> !item.settingsEqual(existing) && result.settingsEqual(existing))) {
			machine.getLTLFormulas().set(machine.getLTLFormulas().indexOf(item), result);
			currentProject.setSaved(false);
			setHandleItem(new LTLHandleItem<>(HandleType.CHANGE, result));
			formulaChecker.checkFormula(result, this);
		} else {
			this.close();
			resultHandler.showAlreadyExists(AbstractResultHandler.ItemType.FORMULA);
		}
	}

	@FXML
	private void cancel() {
		if(formulaChecker.isRunning()) {
			formulaChecker.cancel();
		}
		this.close();
	}

}
