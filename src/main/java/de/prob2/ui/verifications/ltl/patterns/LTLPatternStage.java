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
import netscape.javascript.JSObject;

import java.util.stream.Collectors;

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
		final JSObject editor = (JSObject) engine.executeScript("LtlEditor.cm");
		String code = editor.call("getValue").toString();
		LTLPatternItem item = new LTLPatternItem(code, taDescription.getText());
		if(handleItem.getHandleType() == HandleType.ADD) {
			addItem(currentProject.getCurrentMachine(), item);
		} else {
			changeItem(handleItem.getItem(), item);
		}
	}
	
	@Override
	protected void addItem(Machine machine, LTLPatternItem item) {
		patternParser.parsePattern(item, machine);
		if(!machine.getLTLPatterns().contains(item)) {
			patternParser.addPattern(item, machine);
			machine.addLTLPattern(item);
			setHandleItem(new LTLHandleItem<>(HandleType.CHANGE, item));
			showErrors((LTLCheckingResultItem) item.getResultItem());
		} else {
			resultHandler.showAlreadyExists(AbstractResultHandler.ItemType.PATTERN);
		}
	}
	
	@Override
	protected void changeItem(LTLPatternItem item, LTLPatternItem result) {
		Machine machine = currentProject.getCurrentMachine();
		patternParser.removePattern(item, machine);
		patternParser.parsePattern(result, machine);
		if(!machine.getLTLPatterns().stream()
				.filter(pattern -> !pattern.equals(item))
				.collect(Collectors.toList())
				.contains(result)) {
			machine.getLTLPatterns().set(machine.getLTLPatterns().indexOf(item), result);
			patternParser.addPattern(result, machine);
			currentProject.setSaved(false);
			setHandleItem(new LTLHandleItem<>(HandleType.CHANGE, result));
			showErrors((LTLCheckingResultItem) result.getResultItem());
		} else {
			resultHandler.showAlreadyExists(AbstractResultHandler.ItemType.PATTERN);
		}
	}
	
}
