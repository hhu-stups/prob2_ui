package de.prob2.ui.verifications.ltl.patterns;

import com.google.inject.Inject;

import de.prob2.ui.internal.AbstractResultHandler;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.layout.FontSize;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.verifications.ltl.LTLCheckingResultItem;
import de.prob2.ui.verifications.ltl.LTLHandleItem;
import de.prob2.ui.verifications.ltl.LTLHandleItem.HandleType;
import de.prob2.ui.verifications.ltl.LTLItemStage;
import de.prob2.ui.verifications.ltl.LTLResultHandler;
import de.prob2.ui.verifications.ltl.patterns.builtins.LTLBuiltinsStage;

import javafx.fxml.FXML;

public class LTLPatternStage extends LTLItemStage<LTLPatternItem> {
	private final LTLPatternParser patternParser;
	
	@Inject
	public LTLPatternStage(final StageManager stageManager, final CurrentProject currentProject, final FontSize fontSize,
			final LTLPatternParser patternParser, final LTLResultHandler resultHandler, final LTLBuiltinsStage builtinsStage) {
		super(currentProject, fontSize, resultHandler, builtinsStage);
		this.patternParser = patternParser;
		stageManager.loadFXML(this, "ltlpattern_stage.fxml");
	}
	
	@FXML
	private void applyPattern() {
		String code = taCode.getText();
		LTLPatternItem item = patternParser.parsePattern(taDescription.getText(), code, currentProject.getCurrentMachine());
		if(handleItem.getHandleType() == HandleType.ADD) {
			addItem(currentProject.getCurrentMachine(), item);
		} else {
			changeItem(handleItem.getItem(), item);
		}
	}
	
	private void addItem(Machine machine, LTLPatternItem item) {
		if(machine.getLTLPatterns().stream().noneMatch(item::settingsEqual)) {
			patternParser.addPattern(item, machine);
			machine.getLTLPatterns().add(item);
			setHandleItem(new LTLHandleItem<>(HandleType.CHANGE, item));
			showErrors((LTLCheckingResultItem) item.getResultItem());
		} else {
			this.close();
			resultHandler.showAlreadyExists(AbstractResultHandler.ItemType.PATTERN);
		}
	}
	
	private void changeItem(LTLPatternItem item, LTLPatternItem result) {
		Machine machine = currentProject.getCurrentMachine();
		patternParser.removePattern(item, machine);
		if(machine.getLTLPatterns().stream().noneMatch(existing -> !existing.settingsEqual(item) && existing.settingsEqual(result))) {
			machine.getLTLPatterns().set(machine.getLTLPatterns().indexOf(item), result);
			patternParser.addPattern(result, machine);
			currentProject.setSaved(false);
			setHandleItem(new LTLHandleItem<>(HandleType.CHANGE, result));
			showErrors((LTLCheckingResultItem) result.getResultItem());
		} else {
			this.close();
			resultHandler.showAlreadyExists(AbstractResultHandler.ItemType.PATTERN);
		}
	}
	
}
