package de.prob2.ui.formula;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;


import de.prob.animator.domainobjects.EvaluationException;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.exception.ProBError;
import de.prob2.ui.internal.DynamicCommandStatusBar;
import de.prob2.ui.internal.StageManager;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;

import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;

import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class FormulaStage extends Stage {
	private static final Logger logger = LoggerFactory.getLogger(FormulaStage.class);

	@FXML
	private TextField tfFormula;

	@FXML
	private ScrollPane formulaPane;
	
	@FXML
	private DynamicCommandStatusBar statusBar;
	
	private final Injector injector;
	
	private final ResourceBundle bundle;
	
	private FormulaView formulaView;
	
	private final ObjectProperty<Thread> currentThread;

	@Inject
	public FormulaStage(final StageManager stageManager, final Injector injector, final ResourceBundle bundle) {
		this.injector = injector;
		this.bundle = bundle;
		stageManager.loadFXML(this, "formula_view.fxml");
		this.currentThread = new SimpleObjectProperty<>(this, "currentThread", null);
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
		showFormula(tfFormula.getText());
	}
	
	public void showFormula(String formula) {
		FormulaGenerator formulaGenerator = injector.getInstance(FormulaGenerator.class);
		try {
			Thread thread = new Thread(() -> {
				Platform.runLater(() -> statusBar.setText(bundle.getString("statusBar.loading")));
				formulaView = formulaGenerator.parseAndShowFormula(formula);
				Platform.runLater(() -> {
					formulaPane.setContent(formulaView);
					tfFormula.getStyleClass().remove("text-field-error");
					statusBar.setText("");
				});
			});
			currentThread.set(thread);
			thread.start();
		} catch (EvaluationException | ProBError exception) {
			logger.error("Evaluation of formula failed", exception);
			tfFormula.getStyleClass().add("text-field-error");
		}
	}
	
	public void showFormula(final IEvalElement formula) {
		FormulaGenerator formulaGenerator = injector.getInstance(FormulaGenerator.class);
		try {
			Thread thread = new Thread(() -> {
				Platform.runLater(() -> statusBar.setText(bundle.getString("statusBar.loading")));
				formulaView = formulaGenerator.showFormula(formula);
				Platform.runLater(() -> {
					tfFormula.setText(formula.getCode());
					formulaPane.setContent(formulaView);
					tfFormula.getStyleClass().remove("text-field-error");
					statusBar.setText("");
				});
			});
			currentThread.set(thread);
			thread.start();
		} catch (EvaluationException | ProBError exception) {
			logger.error("Evaluation of formula failed", exception);
			tfFormula.getStyleClass().add("text-field-error");
		}
	}
	
	@FXML
	private void cancel() {
		if (currentThread.get() != null) {
			currentThread.get().interrupt();
			currentThread.set(null);
		}
		if(formulaView != null) {
			formulaView.getChildren().clear();
		}
		statusBar.setText("");
	}
	
	@FXML
	private void zoomIn() {
		if(formulaView == null) {
			return;
		}
		formulaView.zoomByFactor(1.3);
		formulaPane.setHvalue(formulaPane.getHvalue() * 1.3);
		formulaPane.setVvalue(formulaPane.getVvalue() * 1.3);
	}
	
	@FXML
	private void zoomOut() {
		if(formulaView == null) {
			return;
		}
		formulaView.zoomByFactor(0.8);
		formulaPane.setHvalue(formulaPane.getHvalue() * 0.8);
		formulaPane.setVvalue(formulaPane.getVvalue() * 0.8);
	}
	
	@FXML
	private void handleClose() {
		this.close();
	}
	
}
