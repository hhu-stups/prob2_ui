package de.prob2.ui.formula;

import java.util.ResourceBundle;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;

import de.prob.animator.domainobjects.EvaluationException;
import de.prob.exception.ProBError;

import de.prob2.ui.internal.StageManager;

import javafx.fxml.FXML;

import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class FormulaStage extends Stage {
	private static final Logger logger = LoggerFactory.getLogger(FormulaStage.class);

	@FXML
	private TextField tfFormula;
	@FXML
	private TextArea exceptionText;
	@FXML
	private Label lbHeader;
	@FXML
	private FontAwesomeIconView icon;

	@FXML
	private ScrollPane formulaPane;
	
	private final Injector injector;
	private final ResourceBundle bundle;

	@Inject
	public FormulaStage(StageManager stageManager, Injector injector, ResourceBundle bundle) {
		this.injector = injector;
		this.bundle = bundle;
		stageManager.loadFXML(this, "formula_view.fxml");
	}

	@FXML
	public void initialize() {
		tfFormula.setOnKeyReleased(e -> {
			if (e.getCode() == KeyCode.ENTER) {
				apply();
			}
		});
	}

	@FXML
	private void apply() {
		FormulaGenerator formulaGenerator = injector.getInstance(FormulaGenerator.class);
		try {
			formulaPane.setContent((formulaGenerator.parseAndShowFormula(tfFormula.getText())));
		} catch (EvaluationException | ProBError exception) {
			logger.error("Evaluation of formula failed", exception);
			lbHeader.setText(bundle.getString("formula.input.couldNotVisualize"));
			tfFormula.getStyleClass().add("text-field-error");
			icon.setIcon(FontAwesomeIcon.MINUS_CIRCLE);
		}
	}
	
	
}
