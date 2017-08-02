package de.prob2.ui.formula;

import com.google.inject.Inject;
import com.google.inject.Injector;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import de.prob.animator.domainobjects.EvaluationException;
import de.prob.exception.ProBError;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.layout.FontSize;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.stage.Modality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;

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

		Platform.runLater(() -> {
			DialogPane dialogPane = this.getDialogPane();
			Hyperlink detailsButton= (Hyperlink) dialogPane.lookup(".details-button");
			FontAwesomeIconView detailsInitIcon = new FontAwesomeIconView(
					dialogPane.isExpanded() ? FontAwesomeIcon.CHEVRON_UP: FontAwesomeIcon.CHEVRON_DOWN);
			detailsInitIcon.setStyleClass("icon-dark");
			detailsInitIcon.glyphSizeProperty().bind(fontsize);
			detailsButton.setGraphic(detailsInitIcon);
			dialogPane.expandedProperty().addListener((observable, from, to) -> {
				FontAwesomeIconView detailsIcon = new FontAwesomeIconView(
						to ? FontAwesomeIcon.CHEVRON_UP: FontAwesomeIcon.CHEVRON_DOWN);
				detailsIcon.setStyleClass("icon-dark");
				detailsIcon.glyphSizeProperty().bind(fontsize);
				detailsButton.setGraphic(detailsIcon);
			});
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
			this.getDialogPane().setExpanded(true);
			lbHeader.setText("Could not parse or visualize formula");
			tfFormula.getStyleClass().add("text-field-error");
			icon.setIcon(FontAwesomeIcon.MINUS_CIRCLE);
		}
	}
}
