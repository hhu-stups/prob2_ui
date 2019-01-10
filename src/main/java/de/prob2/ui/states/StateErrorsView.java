package de.prob2.ui.states;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob.animator.domainobjects.StateError;

import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentTrace;

import javafx.fxml.FXML;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.AnchorPane;

@FXMLInjected
@Singleton
public final class StateErrorsView extends AnchorPane {
	private final class StateErrorCell extends ListCell<StateError> {
		private StateErrorCell() {
			super();
			
			this.setOnMouseClicked(event -> {
				if (event.getClickCount() == 2 && event.getButton() == MouseButton.PRIMARY && this.getItem() != null && !this.isEmpty()) {
					final FullValueStage stage = injector.getInstance(FullValueStage.class);
					stage.setTitle(this.getItem().getShortDescription());
					stage.setCurrentValue(this.getItem().getLongDescription());
					stage.setPreviousValue(null);
					stage.setFormattingEnabled(false);
					stage.show();
				}
			});
		}
		
		@Override
		protected void updateItem(final StateError item, final boolean empty) {
			super.updateItem(item, empty);
			
			if (item == null || empty) {
				this.setText(null);
			} else {
				this.setText(item.getShortDescription());
			}
		}
	}
	
	@FXML private ListView<StateError> lv;
	
	private final Injector injector;
	private final CurrentTrace currentTrace;
	
	@Inject
	private StateErrorsView(final Injector injector, final CurrentTrace currentTrace, final StageManager stageManager) {
		super();
		
		this.injector = injector;
		this.currentTrace = currentTrace;
		
		stageManager.loadFXML(this, "state_errors_view.fxml");
	}
	
	@FXML
	private void initialize() {
		this.lv.setCellFactory(view -> new StateErrorsView.StateErrorCell());
		
		this.currentTrace.addListener((o, from, to) -> {
			if (to == null) {
				this.lv.getItems().clear();
			} else {
				this.lv.getItems().setAll(to.getCurrentState().getStateErrors());
			}
		});
	}
}
