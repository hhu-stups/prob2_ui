package de.prob2.ui.statusbar;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob.animator.domainobjects.StateError;
import de.prob.statespace.State;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentTrace;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

@FXMLInjected
@Singleton
public final class ErrorStatusStage extends Stage {
	private static final class StateErrorCell extends ListCell<StateError> {
		private StateErrorCell() {
			super();
		}
		
		@Override
		protected void updateItem(final StateError item, final boolean empty) {
			super.updateItem(item, empty);
			
			if (empty) {
				this.setText(null);
			} else {
				this.setText(item.getShortDescription());
			}
		}
	}
	
	@FXML
	private Label placeholderLabel;
	@FXML
	private VBox errorsBox;
	@FXML
	private Label invariantOkLabel;
	@FXML
	private Label deadlockLabel;
	@FXML
	private Label otherStateErrorsLabel;
	@FXML
	private SplitPane otherStateErrorsPane;
	@FXML
	private ListView<StateError> errorsList;
	@FXML
	private TextArea descriptionTextArea;
	
	private final I18n i18n;
	private final CurrentTrace currentTrace;
	
	@Inject
	private ErrorStatusStage(final StageManager stageManager, final I18n i18n, final CurrentTrace currentTrace) {
		super();
		
		this.i18n = i18n;
		this.currentTrace = currentTrace;
		
		stageManager.loadFXML(this, "error_status_stage.fxml", this.getClass().getName());
	}
	
	@FXML
	private void initialize() {
		this.errorsList.setCellFactory(view -> new ErrorStatusStage.StateErrorCell());
		this.errorsList.getSelectionModel().selectedItemProperty().addListener((o, from, to) -> {
			if (to == null) {
				this.descriptionTextArea.setText(null);
			} else {
				String desc = to.getLongDescription();
				// TODO Once the error span is returned in a structured format, display it in our usual format and highlight in editor
				// TODO Allow visualizing predicate included in span (once that is returned from Prolog)
				if (to.getSpanDescription() != null) {
					desc += "\n" + to.getSpanDescription();
				}
				this.descriptionTextArea.setText(desc);
			}
		});
		
		this.currentTrace.currentStateProperty().addListener((o, from, to) -> this.update(to));
		this.update(currentTrace.getCurrentState());
	}
	
	private void update(State state) {
		for (final Label label : new Label[] {this.invariantOkLabel, this.deadlockLabel, this.otherStateErrorsLabel}) {
			label.getStyleClass().removeAll("error", "warning", "no-error");
			label.setText(null);
		}
		
		if (state == null) {
			this.errorsList.getItems().clear();
			this.placeholderLabel.setVisible(true);
			this.errorsBox.setVisible(false);
		} else {
			if (!state.isInitialised()) {
				this.invariantOkLabel.setText(i18n.translate("statusbar.errorStatusStage.invariantNotInitialised"));
			} else if (state.isInvariantOk()) {
				// TO DO: isInvariantOk is incorrect for ignored states (SCOPE predicate false)
				this.invariantOkLabel.getStyleClass().add("no-error");
				this.invariantOkLabel.setText(i18n.translate("statusbar.errorStatusStage.invariantOk"));
			} else {
				this.invariantOkLabel.getStyleClass().add("error");
				this.invariantOkLabel.setText(i18n.translate("statusbar.errorStatusStage.invariantNotOk"));
			}
			
			if (state.getOutTransitions().isEmpty()) {
				// TO DO: this test is incorrect for ignored states (SCOPE predicate false) or if MAX_OPERATIONS==0
				this.deadlockLabel.getStyleClass().add("warning");
				this.deadlockLabel.setText(i18n.translate("statusbar.errorStatusStage.deadlocked"));
			} else {
				this.deadlockLabel.getStyleClass().add("no-error");
				this.deadlockLabel.setText(i18n.translate("statusbar.errorStatusStage.notDeadlocked"));
			}
			
			if (state.getStateErrors().isEmpty()) {
				this.otherStateErrorsLabel.getStyleClass().add("no-error");
				this.otherStateErrorsLabel.setText(i18n.translate("statusbar.errorStatusStage.noOtherStateErrors"));
				this.otherStateErrorsPane.setVisible(false);
				this.otherStateErrorsPane.setManaged(false);
			} else {
				this.otherStateErrorsLabel.getStyleClass().add("error");
				this.otherStateErrorsLabel.setText(i18n.translate("statusbar.errorStatusStage.otherStateErrors"));
				this.otherStateErrorsPane.setVisible(true);
				this.otherStateErrorsPane.setManaged(true);
			}
			this.errorsList.getItems().setAll(state.getStateErrors());
			this.placeholderLabel.setVisible(false);
			this.errorsBox.setVisible(true);
		}
	}
}
