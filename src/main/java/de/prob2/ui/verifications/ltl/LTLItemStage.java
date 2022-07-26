package de.prob2.ui.verifications.ltl;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import de.prob.animator.domainobjects.ErrorItem;
import de.prob2.ui.helpsystem.HelpButton;
import de.prob2.ui.layout.BindableGlyph;
import de.prob2.ui.layout.FontSize;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.CheckingResultItem;
import de.prob2.ui.verifications.ltl.patterns.builtins.LTLBuiltinsStage;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

import org.fxmisc.richtext.CodeArea;

public abstract class LTLItemStage<T extends ILTLItem> extends Stage {
	
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
	
	protected final LTLResultHandler resultHandler;
	
	protected final LTLBuiltinsStage builtinsStage;
	
	protected LTLHandleItem<T> handleItem;
	
	public LTLItemStage(final CurrentProject currentProject, final FontSize fontSize, final LTLResultHandler resultHandler, final LTLBuiltinsStage builtinsStage) {
		super();
		this.currentProject = currentProject;
		this.fontSize = fontSize;
		this.resultHandler = resultHandler;
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
	
	public void clear() {
		this.taDescription.clear();
	}
	
	public void setHandleItem(LTLHandleItem<T> handleItem) {
		this.handleItem = handleItem;
	}
	
	public void showErrors(CheckingResultItem resultItem) {
		//resultItem can be null when LTL model checking is interrupted
		if(resultItem == null || resultItem.getChecked() != Checked.PARSE_ERROR) {
			this.close();
			return;
		}
		taErrors.setText(Arrays.stream(resultItem.getMessageParams())
			.map(Object::toString)
			.collect(Collectors.joining("\n")));
		markText(resultItem);
	}
	
	private void markText(CheckingResultItem resultItem) {
		if(resultItem instanceof LTLCheckingResultItem) {
			final List<ErrorItem> errorMarkers = ((LTLCheckingResultItem)resultItem).getErrorMarkers();
			if (!errorMarkers.isEmpty() && !errorMarkers.get(0).getLocations().isEmpty()) {
				// TODO Implement proper error highlighting like in BEditor
				final ErrorItem.Location location = errorMarkers.get(0).getLocations().get(0);
				final int line = location.getStartLine() - 1;
				taCode.selectRange(line, location.getStartColumn(), line, location.getEndColumn());
			}
		}
	}

	public void setErrors(String text) {
		taErrors.setText(text);
	}
}
