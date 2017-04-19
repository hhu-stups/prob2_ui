package de.prob2.ui.formula;

import java.io.PrintWriter;
import java.io.StringWriter;

import com.google.inject.Inject;
import com.google.inject.Injector;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;

import de.prob.animator.domainobjects.EvaluationException;
import de.prob.exception.ProBError;

import de.prob2.ui.internal.StageManager;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.stage.Modality;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FormulaInputStage extends Stage {
	
	private static final Logger logger = LoggerFactory.getLogger(FormulaInputStage.class);
	
	private final Injector injector;
	
	@FXML
	private DialogPane parent;
	
	@FXML
	private TextField tfFormula;
	
	@FXML
	private TextArea exceptionText;
	
	@FXML
	private Label lbHeader;
	
	@FXML
	private FontAwesomeIconView icon;
	
	
	@Inject
	public FormulaInputStage(StageManager stageManager, Injector injector) {
		stageManager.loadFXML(this, "formula_input_stage.fxml");
		this.injector = injector;
		this.initModality(Modality.APPLICATION_MODAL);
		setButtonAction();
	}

	private void setButtonAction() {
		Button btapply = (Button) parent.lookupButton(ButtonType.APPLY);
		Button btcancel = (Button) parent.lookupButton(ButtonType.CANCEL);
		btapply.setOnMouseClicked(e->apply());
		btcancel.setOnMouseClicked(e->close());
		
		tfFormula.setOnKeyReleased(e-> {
			if (e.getCode() == KeyCode.ENTER) {
				apply();
			}
		});
	}
	
	private void apply() {
		FormulaGenerator formulaGenerator = injector.getInstance(FormulaGenerator.class);
		try {
			formulaGenerator.parseAndShowFormula(tfFormula.getText());
			close();
		} catch (EvaluationException | ProBError exception) {
			logger.error("Evaluation of formula failed", exception);
			StringWriter sw = new StringWriter();
			try (PrintWriter pw = new PrintWriter(sw)) {
				exception.printStackTrace(pw);
				exceptionText.setText(sw.toString());
			}
			parent.setExpanded(true);
			lbHeader.setText("Could not parse or visualize formula");
			icon.setIcon(FontAwesomeIcon.MINUS_CIRCLE);
		}
	}
}
