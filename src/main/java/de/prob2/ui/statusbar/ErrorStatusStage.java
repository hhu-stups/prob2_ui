package de.prob2.ui.statusbar;

import java.util.ResourceBundle;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob.animator.domainobjects.StateError;
import de.prob.statespace.Trace;
import de.prob2.ui.internal.FXMLInjected;
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
	private Label otherStateErrorsLabel;
	@FXML
	private SplitPane otherStateErrorsPane;
	@FXML
	private ListView<StateError> errorsList;
	@FXML
	private TextArea descriptionTextArea;
	
	private final ResourceBundle bundle;
	private final CurrentTrace currentTrace;
	
	@Inject
	private ErrorStatusStage(final StageManager stageManager, final ResourceBundle bundle, final CurrentTrace currentTrace) {
		super();
		
		this.bundle = bundle;
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
				this.descriptionTextArea.setText(to.getLongDescription());
			}
		});
		
		this.currentTrace.addListener((o, from, to) -> this.update(to));
		this.update(currentTrace.get());
	}
	
	private void update(final Trace to) {
		this.invariantOkLabel.getStyleClass().removeAll("error", "no-error");
		this.invariantOkLabel.setText(null);
		this.otherStateErrorsLabel.getStyleClass().removeAll("error", "no-error");
		this.otherStateErrorsLabel.setText(null);
		
		if (to == null) {
			this.errorsList.getItems().clear();
			this.placeholderLabel.setVisible(true);
			this.errorsBox.setVisible(false);
		} else {
			if (!to.getCurrentState().isInitialised()) {
				this.invariantOkLabel.setText(this.bundle.getString("statusbar.errorStatusStage.invariantNotInitialised"));
			} else if (to.getCurrentState().isInvariantOk()) {
				this.invariantOkLabel.getStyleClass().add("no-error");
				this.invariantOkLabel.setText(this.bundle.getString("statusbar.errorStatusStage.invariantOk"));
			} else {
				this.invariantOkLabel.getStyleClass().add("error");
				this.invariantOkLabel.setText(this.bundle.getString("statusbar.errorStatusStage.invariantNotOk"));
			}
			
			if (to.getCurrentState().getStateErrors().isEmpty()) {
				this.otherStateErrorsLabel.getStyleClass().add("no-error");
				this.otherStateErrorsLabel.setText(this.bundle.getString("statusbar.errorStatusStage.noOtherStateErrors"));
				this.otherStateErrorsPane.setVisible(false);
				this.otherStateErrorsPane.setManaged(false);
			} else {
				this.otherStateErrorsLabel.getStyleClass().add("error");
				this.otherStateErrorsLabel.setText(this.bundle.getString("statusbar.errorStatusStage.otherStateErrors"));
				this.otherStateErrorsPane.setVisible(true);
				this.otherStateErrorsPane.setManaged(true);
			}
			this.errorsList.getItems().setAll(to.getCurrentState().getStateErrors());
			this.placeholderLabel.setVisible(false);
			this.errorsBox.setVisible(true);
		}
	}
}
