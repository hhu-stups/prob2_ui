package de.prob2.ui.formula;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Injector;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import de.prob.animator.domainobjects.EvaluationException;
import de.prob.exception.ProBError;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.layout.FontSize;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.stage.Modality;

public class FormulaInputDialog extends Dialog<Void> {

	private static final Logger logger = LoggerFactory.getLogger(FormulaInputDialog.class);
	private final Injector injector;

	@FXML
	private TextField tfFormula;
	@FXML
	private TextArea exceptionText;
	@FXML
	private Label lbHeader;
	@FXML
	private FontAwesomeIconView icon;

	@Inject
	public FormulaInputDialog(StageManager stageManager, Injector injector) {
		this.injector = injector;
		this.initModality(Modality.APPLICATION_MODAL);

		stageManager.loadFXML(this, "formula_input_dialog.fxml");
	}

	@FXML
	public void initialize() {
		final Button btApply = (Button) this.getDialogPane().lookupButton(ButtonType.APPLY);
		btApply.addEventFilter(ActionEvent.ACTION, event -> {
			apply();
			event.consume();
		});

		tfFormula.setOnKeyReleased(e -> {
			if (e.getCode() == KeyCode.ENTER) {
				apply();
			}
		});

		FontSize fontsize = injector.getInstance(FontSize.class);
		icon.glyphSizeProperty().bind(fontsize.multiply(6));
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
			this.getDialogPane().setExpanded(true);
			lbHeader.setText("Could not parse or visualize formula");
			tfFormula.getStyleClass().add("text-field-error");
			icon.setIcon(FontAwesomeIcon.MINUS_CIRCLE);
		}
	}
}
