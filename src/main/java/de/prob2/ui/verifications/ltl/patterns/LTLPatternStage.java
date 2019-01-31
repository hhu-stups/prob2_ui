package de.prob2.ui.verifications.ltl.patterns;

import java.util.stream.Collectors;

import com.google.inject.Inject;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.verifications.AbstractResultHandler;
import de.prob2.ui.verifications.CheckingResultItem;
import de.prob2.ui.verifications.ltl.LTLHandleItem;
import de.prob2.ui.verifications.ltl.LTLHandleItem.HandleType;
import de.prob2.ui.verifications.ltl.LTLItemStage;
import de.prob2.ui.verifications.ltl.LTLResultHandler;
import javafx.fxml.FXML;
import netscape.javascript.JSObject;

public class LTLPatternStage extends LTLItemStage<LTLPatternItem> {
		
	@Inject
	public LTLPatternStage(final StageManager stageManager, final CurrentProject currentProject, 
			final LTLPatternParser patternParser, final LTLResultHandler resultHandler) {
		super(currentProject, patternParser, resultHandler);
		stageManager.loadFXML(this, "ltlpattern_stage.fxml");
	}
	
	@FXML
	private void applyPattern() {
		final JSObject editor = (JSObject) engine.executeScript("editor");
		String code = editor.call("getValue").toString();
		if(handleItem.getHandleType() == HandleType.ADD) {
			addItem(currentProject.getCurrentMachine(), new LTLPatternItem(code, taDescription.getText()));
		} else {
			changeItem(handleItem.getItem(), new LTLPatternItem(code, taDescription.getText()));
		}
	}
	
	@Override
	protected void addItem(Machine machine, LTLPatternItem item) {
		LTLPatternParser patternParser = (LTLPatternParser) ltlItemHandler;
		patternParser.parsePattern(item, machine);
		if(!machine.getLTLPatterns().contains(item)) {
			patternParser.addPattern(item, machine);
			machine.addLTLPattern(item);
			updateProject();
			setHandleItem(new LTLHandleItem<LTLPatternItem>(HandleType.CHANGE, item));
			CheckingResultItem resultItem = item.getResultItem();
			if(resultItem != null) {
				taErrors.setText(resultItem.getMessage());
				return;
			}
			this.close();
		} else {
			resultHandler.showAlreadyExists(AbstractResultHandler.ItemType.PATTERN);
		}
	}
	
	@Override
	protected void changeItem(LTLPatternItem item, LTLPatternItem result) {
		LTLPatternParser patternParser = (LTLPatternParser) ltlItemHandler;
		Machine machine = currentProject.getCurrentMachine();
		patternParser.removePattern(item, machine);
		patternParser.parsePattern(result, machine);
		if(!machine.getLTLPatterns().stream()
				.filter(pattern -> !pattern.equals(item))
				.collect(Collectors.toList())
				.contains(result)) {
			item.setData(result.getName(), result.getDescription(), result.getCode());
			patternParser.addPattern(item, machine);
			currentProject.setSaved(false);
			setHandleItem(new LTLHandleItem<LTLPatternItem>(HandleType.CHANGE, result));
			CheckingResultItem resultItem = result.getResultItem();
			if(resultItem != null) {
				taErrors.setText(resultItem.getMessage());
				return;
			}
			this.close();
		} else {
			resultHandler.showAlreadyExists(AbstractResultHandler.ItemType.PATTERN);
		}
	}

}
