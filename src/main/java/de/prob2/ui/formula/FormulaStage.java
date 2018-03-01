package de.prob2.ui.formula;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;


import de.prob.animator.domainobjects.EvaluationException;
import de.prob.exception.ProBError;

import de.prob2.ui.internal.StageManager;

import javafx.fxml.FXML;

import javafx.scene.control.ScrollPane;
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
	private ScrollPane formulaPane;
	
	private final Injector injector;
	
	private FormulaView formulaView;

	@Inject
	public FormulaStage(StageManager stageManager, Injector injector) {
		this.injector = injector;
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
			formulaView = formulaGenerator.parseAndShowFormula(tfFormula.getText());
			formulaPane.setContent(formulaView);
			tfFormula.getStyleClass().remove("text-field-error");
		} catch (EvaluationException | ProBError exception) {
			logger.error("Evaluation of formula failed", exception);
			tfFormula.getStyleClass().add("text-field-error");
		}
	}
	
	@FXML
	private void zoomIn() {
		formulaView.zoomByFactor(1.3);
		formulaPane.setHvalue(formulaPane.getHvalue() * 1.3);
		formulaPane.setVvalue(formulaPane.getVvalue() * 1.3);
	}
	
	@FXML
	private void zoomOut() {
		formulaView.zoomByFactor(0.8);
		formulaPane.setHvalue(formulaPane.getHvalue() * 0.8);
		formulaPane.setVvalue(formulaPane.getVvalue() * 0.8);
	}
	
}
