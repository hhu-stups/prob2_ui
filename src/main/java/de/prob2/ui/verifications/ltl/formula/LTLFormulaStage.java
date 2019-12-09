package de.prob2.ui.verifications.ltl.formula;

import java.util.stream.Collectors;

import com.google.inject.Inject;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.internal.AbstractResultHandler;
import de.prob2.ui.verifications.ltl.LTLCheckingResultItem;
import de.prob2.ui.verifications.ltl.LTLHandleItem;
import de.prob2.ui.verifications.ltl.LTLItemStage;
import de.prob2.ui.verifications.ltl.LTLResultHandler;
import de.prob2.ui.verifications.ltl.patterns.builtins.LTLBuiltinsStage;
import de.prob2.ui.verifications.ltl.LTLHandleItem.HandleType;
import javafx.fxml.FXML;
import netscape.javascript.JSObject;

public class LTLFormulaStage extends LTLItemStage<LTLFormulaItem> {
			
	@Inject
	public LTLFormulaStage(final StageManager stageManager, final CurrentProject currentProject, 
			final LTLFormulaChecker formulaChecker, final LTLResultHandler resultHandler, final LTLBuiltinsStage builtinsStage) {
		super(currentProject, formulaChecker, resultHandler, builtinsStage);
		stageManager.loadFXML(this, "ltlformula_stage.fxml"); 
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
	
	@Override
	protected void addItem(Machine machine, LTLFormulaItem item) {
		LTLFormulaChecker formulaChecker = (LTLFormulaChecker) ltlItemHandler;
		if(!machine.getLTLFormulas().contains(item)) {
			machine.addLTLFormula(item);
			setHandleItem(new LTLHandleItem<LTLFormulaItem>(HandleType.CHANGE, item));
			formulaChecker.checkFormula(item, this);
			showErrors((LTLCheckingResultItem) item.getResultItem());
		} else {
			resultHandler.showAlreadyExists(AbstractResultHandler.ItemType.FORMULA);
		}
	}
	
	@Override
	protected void changeItem(LTLFormulaItem item, LTLFormulaItem result) {
		LTLFormulaChecker formulaChecker = (LTLFormulaChecker) ltlItemHandler;
		Machine machine = currentProject.getCurrentMachine();
		if(!machine.getLTLFormulas().stream()
				.filter(formula -> !formula.equals(item))
				.collect(Collectors.toList())
				.contains(result)) {
			item.setData(result.getName(), result.getDescription(), result.getCode());
			item.setCounterExample(null);
			item.setResultItem(null);
			currentProject.setSaved(false);
			setHandleItem(new LTLHandleItem<LTLFormulaItem>(HandleType.CHANGE, item));
			formulaChecker.checkFormula(item, this);
			showErrors((LTLCheckingResultItem) item.getResultItem());
		} else {
			resultHandler.showAlreadyExists(AbstractResultHandler.ItemType.FORMULA);
		}
	}
	
	public void setErrors(String text) {
		taErrors.setText(text);
	}

}
