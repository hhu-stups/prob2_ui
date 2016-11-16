package de.prob2.ui.formula;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import com.google.inject.Inject;
import com.google.inject.Injector;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;

import de.prob.animator.domainobjects.EvaluationException;
import de.prob.exception.ProBError;

import de.prob2.ui.prob2fx.CurrentStage;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FormulaInputStage extends Stage {
	
	private static final Logger logger = LoggerFactory.getLogger(FormulaInputStage.class);
	
	private final Injector injector;
	
	@FXML
	private DialogPane parent;
	
	@FXML
	private TextField tf_formula;
	
	@FXML
	private TextArea exceptionText;
	
	@FXML
	private Label lb_header;
	
	@FXML
	private FontAwesomeIconView icon;
	
	
	@Inject
	public FormulaInputStage(FXMLLoader loader, CurrentStage currentStage, Injector injector) {
		loader.setLocation(getClass().getResource("formula_input_stage.fxml"));
		loader.setRoot(this);
		loader.setController(this);
		try {
			loader.load();
		} catch (IOException e) {
			logger.error("loading fxml failed", e);
		}
		currentStage.register(this);
		this.injector = injector;
		setButtonAction();
	}

	private void setButtonAction() {
		Button btapply = (Button) parent.lookupButton(ButtonType.APPLY);
		Button btcancel = (Button) parent.lookupButton(ButtonType.CANCEL);
		btapply.setOnMouseClicked(e->apply());
		btcancel.setOnMouseClicked(e->close());
		
		tf_formula.setOnKeyReleased(e-> {
			if(e.getCode() == KeyCode.ENTER) {
				apply();
			}
		});
	}
	
	private void apply() {
		FormulaGenerator formulaGenerator = injector.getInstance(FormulaGenerator.class);
		try {
			formulaGenerator.parseAndShowFormula(tf_formula.getText());
			close();
		} catch (EvaluationException | ProBError exception) {
			logger.error("Evaluation of formula failed", exception);
			StringWriter sw = new StringWriter();
			try (PrintWriter pw = new PrintWriter(sw)) {
				exception.printStackTrace(pw);
				exceptionText.setText(sw.toString());
			}
			parent.setExpanded(true);
			lb_header.setText("Could not parse or visualize formula");
			tf_formula.getStyleClass().add("text-field-error");
			icon.setIcon(FontAwesomeIcon.MINUS_CIRCLE);
		}
	}
}
