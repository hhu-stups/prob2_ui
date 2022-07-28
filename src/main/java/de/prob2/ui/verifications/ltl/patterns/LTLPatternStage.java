package de.prob2.ui.verifications.ltl.patterns;

import com.google.inject.Inject;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.layout.FontSize;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.ltl.LTLCheckingResultItem;
import de.prob2.ui.verifications.ltl.LTLItemStage;
import de.prob2.ui.verifications.ltl.patterns.builtins.LTLBuiltinsStage;

import javafx.fxml.FXML;

public class LTLPatternStage extends LTLItemStage {
	private final LTLPatternParser patternParser;
	
	private LTLPatternItem result;
	
	@Inject
	public LTLPatternStage(final StageManager stageManager, final CurrentProject currentProject, final FontSize fontSize,
			final LTLPatternParser patternParser, final LTLBuiltinsStage builtinsStage) {
		super(currentProject, fontSize, builtinsStage);
		this.patternParser = patternParser;
		this.result = null;
		stageManager.loadFXML(this, "ltlpattern_stage.fxml");
	}
	
	public LTLPatternItem getResult() {
		return this.result;
	}
	
	public void setData(final LTLPatternItem item) {
		taCode.replaceText(item.getCode());
		taDescription.setText(item.getDescription());
	}
	
	@FXML
	private void applyPattern() {
		this.result = null;
		String code = taCode.getText();
		LTLPatternItem item = patternParser.parsePattern(taDescription.getText(), code, currentProject.getCurrentMachine());
		final LTLCheckingResultItem resultItem = (LTLCheckingResultItem) item.getResultItem();
		if (resultItem.getChecked() == Checked.PARSE_ERROR) {
			showErrors(resultItem.getErrorMarkers());
		} else {
			this.result = item;
			this.close();
		}
	}
}
