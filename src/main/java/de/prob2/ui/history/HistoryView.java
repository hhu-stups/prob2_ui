package de.prob2.ui.history;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.prob.statespace.Transition;
import de.prob2.ui.prob2fx.CurrentTrace;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.Text;

@Singleton
public class HistoryView extends AnchorPane {
	private static class TransitionCell extends ListCell<HistoryItem> {
		@Override
		protected void updateItem(HistoryItem item, boolean empty) {
			super.updateItem(item, empty);
			if (item == null) {
				setGraphic(new Text());
			} else {
				String content;
				if (item.root) {
					content = "---root---";
				} else {
					content = item.transition.getPrettyRep();
				}
				
				Text text = new Text(content);
				text.setDisable(true);
				
				switch (item.status) {
					case PAST:
						text.setId("past");
						break;
					
					case FUTURE:
						text.setId("future");
						break;
					
					default:
						text.setId("present");
						break;
				}
				setGraphic(text);
			}
		}
	}
	
	@FXML
	private ListView<HistoryItem> lv_history;

	@FXML
	private ToggleButton tb_reverse;

	@FXML
	private Button btprevious;

	@FXML
	private Button btforward;
	
	private final CurrentTrace currentTrace;

	@Inject
	private HistoryView(FXMLLoader loader, CurrentTrace currentTrace) {
		this.currentTrace = currentTrace;
		loader.setLocation(getClass().getResource("history_view.fxml"));
		loader.setRoot(this);
		loader.setController(this);
		try {
			loader.load();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@FXML
	public void initialize() {
		currentTrace.addListener((observable, from, to) -> {
			lv_history.getItems().clear();
			if (to != null) {
				int currentPos = to.getCurrent().getIndex();
				
				if (currentPos == -1) {
					lv_history.getItems().add(new HistoryItem(HistoryStatus.PRESENT));
				} else {
					lv_history.getItems().add(new HistoryItem(HistoryStatus.PAST));
				}
				
				List<Transition> transitionList = to.getTransitionList();
				for (int i = 0; i < transitionList.size(); i++) {
					HistoryStatus status;
					if (i < currentPos) {
						status = HistoryStatus.PAST;
					} else if (i > currentPos) {
						status = HistoryStatus.FUTURE;
					} else {
						status = HistoryStatus.PRESENT;
					}
					lv_history.getItems().add(new HistoryItem(transitionList.get(i), status));
				}
			}
			
			if (tb_reverse.isSelected()) {
				Collections.reverse(lv_history.getItems());
			}
		});
		
		btprevious.disableProperty().bind(currentTrace.canGoBackProperty().not());
		btforward.disableProperty().bind(currentTrace.canGoForwardProperty().not());

		lv_history.setCellFactory(item -> new TransitionCell());

		lv_history.setOnMouseClicked(e -> {
			if (currentTrace.exists()) {
				currentTrace.set(currentTrace.get().gotoPosition(getCurrentIndex()));
			}
		});

		lv_history.setOnMouseMoved(e -> {
			lv_history.setCursor(Cursor.HAND);
		});

		tb_reverse.setOnAction(e -> {
			Collections.reverse(lv_history.getItems());
		});

		btprevious.setOnAction(e -> {
			if (currentTrace.exists()) {
				currentTrace.set(currentTrace.back());
			}
		});

		btforward.setOnAction(e -> {
			if (currentTrace.exists()) {
				currentTrace.set(currentTrace.forward());
			}
		});
	}

	private int getCurrentIndex() {
		int currentPos = lv_history.getSelectionModel().getSelectedIndex();
		int length = lv_history.getItems().size();
		if (tb_reverse.isSelected()) {
			return length - 2 - currentPos;
		} else {
			return currentPos - 1;
		}
	}
}
