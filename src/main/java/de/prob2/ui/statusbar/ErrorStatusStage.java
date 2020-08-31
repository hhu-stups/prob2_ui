package de.prob2.ui.statusbar;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob.animator.domainobjects.StateError;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentTrace;

import javafx.fxml.FXML;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
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
	private ListView<StateError> errorsList;
	@FXML
	private TextArea descriptionTextArea;
	
	private final CurrentTrace currentTrace;
	
	@Inject
	private ErrorStatusStage(final StageManager stageManager, final CurrentTrace currentTrace) {
		super();
		
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
		
		this.currentTrace.addListener((o, from, to) -> {
			if (to == null) {
				this.errorsList.getItems().clear();
			} else {
				this.errorsList.getItems().setAll(to.getCurrentState().getStateErrors());
			}
		});
	}
}
