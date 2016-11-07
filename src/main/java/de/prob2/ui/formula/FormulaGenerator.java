package de.prob2.ui.formula;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import de.prob.animator.command.ExpandFormulaCommand;
import de.prob.animator.command.InsertFormulaForVisualizationCommand;
import de.prob.animator.domainobjects.EvaluationException;
import de.prob.animator.domainobjects.ExpandedFormula;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.exception.ProBError;
import de.prob2.ui.prob2fx.CurrentStage;
import de.prob2.ui.prob2fx.CurrentTrace;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;

@Singleton
public final class FormulaGenerator {
	private static final Logger logger = LoggerFactory.getLogger(FormulaGenerator.class);
	
	private final CurrentTrace currentTrace;
	private final CurrentStage currentStage;
	private final TextInputDialog dialog;

	@Inject
	private FormulaGenerator(final CurrentTrace currentTrace, final CurrentStage currentStage, final TextInputDialog dialog) {
		this.currentTrace = currentTrace;
		this.currentStage = currentStage;
		this.dialog = dialog;
		dialog.setTitle("Enter Formula for Visualization");
		dialog.setHeaderText("Enter Formula for Visualization");
		dialog.setContentText("Enter Formula: ");
		dialog.getDialogPane().getStylesheets().add("prob.css");
		dialog.setResizable(false);
		dialog.getDialogPane().setMinSize(720, 400);
		FontAwesomeIconView icon = new FontAwesomeIconView(FontAwesomeIcon.QUESTION_CIRCLE);
		icon.setGlyphSize(50);
		dialog.setGraphic(icon);
	}

	private ExpandedFormula expandFormula(final IEvalElement formula) {
		if (!currentTrace.getCurrentState().isInitialised()) {
			throw new EvaluationException("Formula evaluation is only possible in an initialized state");
		}

		final InsertFormulaForVisualizationCommand insertCmd = new InsertFormulaForVisualizationCommand(formula);
		currentTrace.getStateSpace().execute(insertCmd);

		final ExpandFormulaCommand expandCmd = new ExpandFormulaCommand(insertCmd.getFormulaId(),
				currentTrace.getCurrentState());
		currentTrace.getStateSpace().execute(expandCmd);

		return expandCmd.getResult();
	}

	public void showFormula(final IEvalElement formula, final TextInputDialog dialog) {
		try {
			ExpandedFormula expanded = expandFormula(formula);
			FormulaView fview = new FormulaView(new FormulaGraph(new FormulaNode(expanded)));
			currentStage.register(fview);
			fview.show();
		} catch (EvaluationException | ProBError e) {
			logger.error("loading fxml failed", e);
			dialog.setHeaderText("Could not visualize formula!\n");
			FontAwesomeIconView icon = (new FontAwesomeIconView(FontAwesomeIcon.MINUS_CIRCLE));
			icon.setGlyphSize(50);
			dialog.setGraphic(icon);
			showParseError(e, dialog);
		}
	}

	public void parseAndShowFormula(final String formula, final TextInputDialog dialog) {
		final IEvalElement parsed;
		try {
			parsed = currentTrace.getModel().parseFormula(formula);
		} catch (EvaluationException e) {
			logger.error("Evaluation of formula failed", e);
			dialog.setHeaderText("Could not parse formula!\n");
			FontAwesomeIconView icon = (new FontAwesomeIconView(FontAwesomeIcon.MINUS_CIRCLE));
			icon.setGlyphSize(50);
			dialog.setGraphic(icon);
			showParseError(e, dialog);
			return;
		}
		showFormula(parsed, dialog);
	}
	
	private void showParseError(final Exception e, final TextInputDialog dialog) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		String exception = sw.toString();
		TextArea exceptionText = new TextArea(exception);
		exceptionText.setEditable(false);
		exceptionText.getStyleClass().add("text-area-error");
		dialog.getDialogPane().setExpandableContent(exceptionText);
		dialog.getDialogPane().setExpanded(true);
		openDialog();
	}
	
	public void openDialog() {
		Optional<String> result = dialog.showAndWait();
		if (result.isPresent()) {
			parseAndShowFormula(result.get(), dialog);
		}
	}
}
