package de.prob2.ui.verifications.ltl.patterns;

import java.util.stream.Collectors;

import com.google.inject.Inject;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.Project;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.verifications.AbstractResultHandler;
import de.prob2.ui.verifications.CheckingResultItem;
import de.prob2.ui.verifications.ltl.LTLHandleItem;
import de.prob2.ui.verifications.ltl.LTLHandleItem.HandleType;
import de.prob2.ui.verifications.ltl.LTLItemStage;
import de.prob2.ui.verifications.ltl.LTLResultHandler;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import netscape.javascript.JSObject;

public class LTLPatternStage extends LTLItemStage {
	
	@FXML
	private TextArea taDescription;
	
	@FXML
	private TextArea taErrors;
	
	private final CurrentProject currentProject;
	
	private final LTLPatternParser patternParser;
	
	private final LTLResultHandler resultHandler;
	
	private LTLHandleItem<LTLPatternItem> handleItem;
		
	@Inject
	public LTLPatternStage(final StageManager stageManager, final CurrentProject currentProject, 
			final LTLPatternParser patternParser, final LTLResultHandler resultHandler) {
		super();
		this.currentProject = currentProject;
		this.patternParser = patternParser;
		this.resultHandler = resultHandler;
		stageManager.loadFXML(this, "ltlpattern_stage.fxml");
	}
	
	@FXML
	private void applyPattern() {
		final JSObject editor = (JSObject) engine.executeScript("editor");
		String code = editor.call("getValue").toString();
		if(handleItem.getHandleType() == HandleType.ADD) {
			addPattern(currentProject.getCurrentMachine(), new LTLPatternItem(code, taDescription.getText()));
		} else {
			changePattern(handleItem.getItem(), new LTLPatternItem(code, taDescription.getText()));
		}
	}
	
	private void addPattern(Machine machine, LTLPatternItem item) {
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
	
	private void changePattern(LTLPatternItem item, LTLPatternItem result) {
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
	
	private void updateProject() {
		currentProject.update(new Project(currentProject.getName(), currentProject.getDescription(), 
				currentProject.getMachines(), currentProject.getPreferences(), currentProject.getLocation()));
	}
	
	public void setHandleItem(LTLHandleItem<LTLPatternItem> handleItem) {
		this.handleItem = handleItem;
	}
}
