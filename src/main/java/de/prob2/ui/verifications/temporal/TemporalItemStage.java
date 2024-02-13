package de.prob2.ui.verifications.temporal;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import de.prob.animator.domainobjects.ErrorItem;
import de.prob2.ui.helpsystem.HelpButton;
import de.prob2.ui.layout.BindableGlyph;
import de.prob2.ui.layout.FontSize;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.verifications.temporal.ltl.patterns.builtins.LTLBuiltinsStage;

import javafx.beans.NamedArg;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

public abstract class TemporalItemStage extends Stage {

	public static final class TemporalFormulaChoiceItem {

		private final TemporalFormulaType type;

		public TemporalFormulaChoiceItem(@NamedArg("type") TemporalFormulaType type) {
			this.type = type;
		}

		public TemporalFormulaType getType() {
			return type;
		}

		@Override
		public String toString() {
			return type.name();
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			TemporalFormulaChoiceItem that = (TemporalFormulaChoiceItem) o;
			return type == that.type;
		}

		@Override
		public int hashCode() {
			return Objects.hash(type);
		}
	}

	@FXML
	protected TemporalFormulaEditor taCode;

	@FXML
	protected TextArea taDescription;

	@FXML
	protected TextArea taErrors;

	@FXML
	protected HelpButton helpButton;

	protected final CurrentProject currentProject;

	protected final FontSize fontSize;

	protected final LTLBuiltinsStage builtinsStage;

	public TemporalItemStage(final CurrentProject currentProject, final FontSize fontSize, final LTLBuiltinsStage builtinsStage) {
		super();
		this.currentProject = currentProject;
		this.fontSize = fontSize;
		this.builtinsStage = builtinsStage;
	}

	@FXML
	public void initialize() {
		this.helpButton.setHelpContent("verification", "LTL");
		((BindableGlyph) this.helpButton.getGraphic()).bindableFontSizeProperty().bind(this.fontSize.fontSizeProperty().multiply(1.2));

		// clear errors when the user types, as all location information will be outdated by then
		this.taCode.textProperty().addListener((observable, oldValue, newValue) -> {
			this.taErrors.clear();
			this.taCode.getErrors().clear();
		});
	}

	@FXML
	protected void showBuiltins() {
		this.builtinsStage.show();
	}

	public void showErrors(final List<ErrorItem> errors) {
		this.taErrors.setText(errors.stream()
			                      .map(ErrorItem::getMessage)
			                      .collect(Collectors.joining("\n")));
		this.taCode.getErrors().setAll(errors);
	}
}
