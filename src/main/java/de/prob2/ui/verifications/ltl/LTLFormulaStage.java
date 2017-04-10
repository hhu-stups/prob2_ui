package de.prob2.ui.verifications.ltl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

import de.be4.ltl.core.parser.LtlParseException;
import de.prob.animator.command.EvaluationCommand;
import de.prob.animator.domainobjects.AbstractEvalResult;
import de.prob.animator.domainobjects.LTL;
import de.prob.check.LTLCounterExample;
import de.prob.check.LTLError;
import de.prob.check.LTLOk;
import de.prob.statespace.State;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentTrace;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

public class LTLFormulaStage extends Stage {
	
	private static final Logger logger = LoggerFactory.getLogger(LTLFormulaStage.class);
		
	@FXML
	private TextArea ta_formula;
	
	private final CurrentTrace currentTrace;
		
	@Inject
	private LTLFormulaStage(final StageManager stageManager, final CurrentTrace currentTrace) {
		stageManager.loadFXML(this, "ltlFormulaStage.fxml");
		this.currentTrace = currentTrace;
	}
	
	@FXML
	private void handleSave() {
		
	}
	
	@FXML
	private void handleClose() {
		this.close();
	}
	
	@FXML
	public void checkFormula() {
		LTL formula = null;
		try {
			formula = new LTL(ta_formula.getText());
		} catch (LtlParseException e) {
			logger.error("Could not parse LTL formula", e);
			return;
		}
		if (currentTrace != null) {
			State stateid = currentTrace.getCurrentState();
			EvaluationCommand lcc = formula.getCommand(stateid);
			currentTrace.getStateSpace().execute(lcc);
			AbstractEvalResult result = lcc.getValue();
			if(result instanceof LTLOk) {
				
			} else if(result instanceof LTLCounterExample || result instanceof LTLError) {
				
			}
		}
	}	
}
