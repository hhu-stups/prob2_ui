package de.prob2.ui.verifications.ltl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Injector;

import de.be4.ltl.core.parser.LtlParseException;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
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
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class LTLFormulaStage extends Stage {
	
	private static final Logger logger = LoggerFactory.getLogger(LTLFormulaStage.class);
		
	@FXML
	private TextArea ta_formula;
	
	@FXML
	private Button checkFormulaButton;
	
	private final CurrentTrace currentTrace;
	
	private LTLFormulaItem item;
	
	private Injector injector;
		
	@Inject
	private LTLFormulaStage(final StageManager stageManager, final Injector injector, final CurrentTrace currentTrace) {
		this.injector = injector;
		this.currentTrace = currentTrace;
		this.item = null;
		stageManager.loadFXML(this, "ltlformula_stage.fxml");
	}
	
	@FXML
	public void initialize() {
		ta_formula.textProperty().addListener((observable, oldValue, newValue) -> {
			item.setFormula(newValue);
		});
		checkFormulaButton.disableProperty().bind(currentTrace.existsProperty().not());
	}
	
	public void setItem(LTLFormulaItem item) {
		this.item = item;
		ta_formula.setText(item.getFormula());
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
		boolean parseError = false;
		try {
			formula = new LTL(ta_formula.getText());
		} catch (LtlParseException e) {
			setCheckedFailed();
			parseError = true;
			logger.error("Could not parse LTL formula", e);
		}
		if (currentTrace != null && !parseError) {
			State stateid = currentTrace.getCurrentState();
			EvaluationCommand lcc = formula.getCommand(stateid);
			currentTrace.getStateSpace().execute(lcc);
			AbstractEvalResult result = lcc.getValue();
			if(result instanceof LTLOk) {
				setCheckedSuccessful();
			} else if(result instanceof LTLCounterExample || result instanceof LTLError) {
				setCheckedFailed();
			}
		}
		injector.getInstance(LTLView.class).refresh();
	}
	
	private void setCheckedSuccessful() {
		FontAwesomeIconView icon = new FontAwesomeIconView(FontAwesomeIcon.CHECK);
		icon.setFill(Color.GREEN);
		item.setStatus(icon);
	}
	
	private void setCheckedFailed() {
		FontAwesomeIconView icon = new FontAwesomeIconView(FontAwesomeIcon.REMOVE);
		icon.setFill(Color.RED);
		item.setStatus(icon);
	}
}
