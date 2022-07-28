package de.prob2.ui.verifications.ltl.formula;

import java.util.stream.Collectors;

import com.google.inject.Inject;

import de.prob.exception.ProBError;
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
import javafx.scene.control.TextField;

public class LTLFormulaStage extends LTLItemStage<LTLFormulaItem> {
	@FXML
	private TextField idTextField;

	@FXML
	private Button applyButton;
	
	private final LTLFormulaChecker formulaChecker;

	private LTLFormulaItem lastItem;
	
	@Inject
	public LTLFormulaStage(
		final StageManager stageManager, final CurrentProject currentProject, final FontSize fontSize,
		final LTLFormulaChecker formulaChecker, final LTLResultHandler resultHandler, final LTLBuiltinsStage builtinsStage
	) {
		super(currentProject, fontSize, resultHandler, builtinsStage);
		this.formulaChecker = formulaChecker;
		stageManager.loadFXML(this, "ltlformula_stage.fxml");
	}

	public void setData(final LTLFormulaItem item) {
		idTextField.setText(item.getId() == null ? "" : item.getId());
		taCode.replaceText(item.getCode());
		taDescription.setText(item.getDescription());
	}

	@FXML
	private void applyFormula() {
		lastItem = null;
		final String id = idTextField.getText().trim().isEmpty() ? null : idTextField.getText();
		String code = taCode.getText();
		if(handleItem.getHandleType() == HandleType.ADD) {
			addItem(currentProject.getCurrentMachine(), new LTLFormulaItem(id, code, taDescription.getText()));
		} else {
			changeItem(handleItem.getItem(), new LTLFormulaItem(id, code, taDescription.getText()));
		}
	}
	
	private void tryParseAndCheckFormula(final Machine machine, final LTLFormulaItem item) {
		lastItem = item;
		try {
			formulaChecker.parseFormula(item.getCode(), machine);
		} catch (ProBError e) {
			this.showErrors(e.getErrors());
			return;
		}
		this.close();
		formulaChecker.checkFormula(item);
	}
	
	private void addItem(Machine machine, LTLFormulaItem item) {
		final LTLFormulaItem toCheck;
		if(machine.getLTLFormulas().stream().noneMatch(item::settingsEqual)) {
			machine.getLTLFormulas().add(item);
			setHandleItem(new LTLHandleItem<>(HandleType.CHANGE, item));
			toCheck = item;
		} else {
			toCheck = machine.getLTLFormulas().stream().filter(item::settingsEqual).collect(Collectors.toList()).get(0);
		}
		this.tryParseAndCheckFormula(machine, toCheck);
	}
	
	private void changeItem(LTLFormulaItem item, LTLFormulaItem result) {
		Machine machine = currentProject.getCurrentMachine();
		if(machine.getLTLFormulas().stream().noneMatch(existing -> !item.settingsEqual(existing) && result.settingsEqual(existing))) {
			machine.getLTLFormulas().set(machine.getLTLFormulas().indexOf(item), result);
			currentProject.setSaved(false);
			setHandleItem(new LTLHandleItem<>(HandleType.CHANGE, result));
			this.tryParseAndCheckFormula(machine, result);
		} else {
			this.close();
			resultHandler.showAlreadyExists(AbstractResultHandler.ItemType.FORMULA);
		}
	}

	@FXML
	private void cancel() {
		this.close();
	}

	public LTLFormulaItem getLastItem() {
		return lastItem;
	}
}
