package de.prob2.ui.verifications.temporal;

import java.util.List;

import de.prob.animator.domainobjects.ErrorItem;
import de.prob2.ui.error.ErrorTableView;
import de.prob2.ui.helpsystem.HelpButton;
import de.prob2.ui.layout.BindableGlyph;
import de.prob2.ui.layout.FontSize;
import de.prob2.ui.verifications.temporal.ltl.patterns.builtins.LTLBuiltinsStage;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

public abstract class TemporalItemStage extends Stage {
	@FXML
	protected TemporalFormulaEditor taCode;

	@FXML
	protected TextArea taDescription;

	@FXML
	protected ErrorTableView errorsTable;

	@FXML
	protected HelpButton helpButton;

	protected final FontSize fontSize;

	protected final LTLBuiltinsStage builtinsStage;

	public TemporalItemStage(final FontSize fontSize, final LTLBuiltinsStage builtinsStage) {
		super();
		this.fontSize = fontSize;
		this.builtinsStage = builtinsStage;
	}

	@FXML
	public void initialize() {
		this.errorsTable.dontSyncWithEditor();
		this.helpButton.setHelpContent("verification", "LTL");
		((BindableGlyph) this.helpButton.getGraphic()).bindableFontSizeProperty().bind(this.fontSize.fontSizeProperty().multiply(1.2));

		// clear errors when the user types, as all location information will be outdated by then
		this.taCode.textProperty().addListener((observable, oldValue, newValue) -> {
			this.errorsTable.getErrorItems().clear();
			this.taCode.getErrors().clear();
		});
	}

	@FXML
	protected void showBuiltins() {
		this.builtinsStage.show();
	}

	public void showErrors(final List<ErrorItem> errors) {
		this.errorsTable.getErrorItems().setAll(errors);
		this.taCode.getErrors().setAll(errors);
	}
}
