package de.prob2.ui.verifications.ltl.formula;

import java.util.stream.Collectors;

import com.google.inject.Inject;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.Project;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.verifications.AbstractResultHandler;
import de.prob2.ui.verifications.ltl.LTLHandleItem;
import de.prob2.ui.verifications.ltl.LTLItemStage;
import de.prob2.ui.verifications.ltl.LTLResultHandler;
import de.prob2.ui.verifications.ltl.LTLHandleItem.HandleType;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import netscape.javascript.JSObject;

public class LTLFormulaStage extends LTLItemStage {
	
	@FXML
	private TextArea taDescription;
	
	private final CurrentProject currentProject;
	
	private final LTLResultHandler resultHandler;
	
	private LTLHandleItem<LTLFormulaItem> handleItem;
			
	@Inject
	public LTLFormulaStage(final StageManager stageManager, final CurrentProject currentProject, 
			final LTLResultHandler resultHandler) {
		super(LTLFormulaItem.class);
		this.currentProject = currentProject;
		this.resultHandler = resultHandler;
		stageManager.loadFXML(this, "ltlformula_stage.fxml"); 
	}
	
	@FXML
	private void applyFormula() {
		final JSObject editor = (JSObject) engine.executeScript("editor");
		String code = editor.call("getValue").toString();
		if(handleItem.getHandleType() == HandleType.ADD) {
			addFormula(currentProject.getCurrentMachine(), new LTLFormulaItem(code, taDescription.getText()));
		} else {
			changeFormula(handleItem.getItem(), new LTLFormulaItem(code, taDescription.getText()));
		}
	}
	
	private void addFormula(Machine machine, LTLFormulaItem item) {
		if(!machine.getLTLFormulas().contains(item)) {
			machine.addLTLFormula(item);
			updateProject();
			this.close();
		} else {
			resultHandler.showAlreadyExists(AbstractResultHandler.ItemType.FORMULA);
		}
	}
	
	private void changeFormula(LTLFormulaItem item, LTLFormulaItem result) {
		Machine machine = currentProject.getCurrentMachine();
		if(!machine.getLTLFormulas().stream()
				.filter(formula -> !formula.equals(item))
				.collect(Collectors.toList())
				.contains(result)) {
			item.setData(result.getName(), result.getDescription(), result.getCode());
			item.setCounterExample(null);
			item.setResultItem(null);
			currentProject.setSaved(false);
			this.close();
		} else {
			resultHandler.showAlreadyExists(AbstractResultHandler.ItemType.FORMULA);
		}
	}
	
	private void updateProject() {
		currentProject.update(new Project(currentProject.getName(), currentProject.getDescription(), 
				currentProject.getMachines(), currentProject.getPreferences(), currentProject.getLocation()));
	}
	
	public void setHandleItem(LTLHandleItem<LTLFormulaItem> handleItem) {
		this.handleItem = handleItem;
	}
	
}
