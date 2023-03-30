package de.prob2.ui.verifications.temporal;

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
import org.fxmisc.richtext.CodeArea;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public abstract class TemporalItemStage extends Stage {

	public static class TemporalFormulaChoiceItem {

		private final TemporalFormulaItem.TemporalType type;

		public TemporalFormulaChoiceItem(@NamedArg("type") TemporalFormulaItem.TemporalType type) {
			this.type = type;
		}

		public TemporalFormulaItem.TemporalType getType() {
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
	protected CodeArea taCode;
	
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
		helpButton.setHelpContent("verification", "LTL");
		((BindableGlyph) helpButton.getGraphic()).bindableFontSizeProperty().bind(fontSize.fontSizeProperty().multiply(1.2));
	}
	
	@FXML
	protected void showBuiltins() {
		builtinsStage.show();
	}
	
	public void showErrors(final List<ErrorItem> errors) {
		taErrors.setText(errors.stream()
			.map(ErrorItem::getMessage)
			.collect(Collectors.joining("\n")));
		markText(errors);
	}
	
	private void markText(final List<ErrorItem> errorMarkers) {
		if (!errorMarkers.isEmpty() && !errorMarkers.get(0).getLocations().isEmpty()) {
			// TODO Implement proper error highlighting like in BEditor
			final ErrorItem.Location location = errorMarkers.get(0).getLocations().get(0);
			final int line = location.getStartLine() - 1;
			taCode.selectRange(line, location.getStartColumn(), line, location.getEndColumn());
		}
	}
}