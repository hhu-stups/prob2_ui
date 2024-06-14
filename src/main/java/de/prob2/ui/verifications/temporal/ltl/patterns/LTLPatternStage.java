package de.prob2.ui.verifications.temporal.ltl.patterns;

import com.google.inject.Inject;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.layout.FontSize;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.verifications.CheckingStatus;
import de.prob2.ui.verifications.temporal.TemporalCheckingResult;
import de.prob2.ui.verifications.temporal.TemporalItemStage;
import de.prob2.ui.verifications.temporal.ltl.patterns.builtins.LTLBuiltinsStage;

import javafx.fxml.FXML;

public final class LTLPatternStage extends TemporalItemStage {
	private final CurrentProject currentProject;

	private LTLPatternItem result;

	@Inject
	public LTLPatternStage(final StageManager stageManager, final CurrentProject currentProject, final FontSize fontSize, final LTLBuiltinsStage builtinsStage) {
		super(fontSize, builtinsStage);
		this.currentProject = currentProject;
		this.result = null;
		stageManager.loadFXML(this, "ltlpattern_stage.fxml");
	}

	public LTLPatternItem getResult() {
		return this.result;
	}

	public void setData(final LTLPatternItem item) {
		this.taCode.replaceText(item.getCode());
		this.taDescription.setText(item.getDescription());
	}

	@FXML
	private void applyPattern() {
		this.result = null;
		String code = this.taCode.getText();
		LTLPatternItem item = LTLPatternParser.parsePattern(this.taDescription.getText(), code, this.currentProject.getCurrentMachine());
		TemporalCheckingResult res = item.getResult();
		if (res.getStatus() == CheckingStatus.INVALID_TASK) {
			showErrors(res.getErrorMarkers());
		} else {
			this.result = item;
			this.close();
		}
	}
}
