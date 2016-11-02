package de.prob2.ui.history;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import com.google.inject.Inject;

import de.prob.statespace.Trace;
import de.prob.statespace.Transition;

import de.prob2.ui.prob2fx.CurrentTrace;

import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.Text;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HistoryView extends AnchorPane {
	private static class TransitionCell extends ListCell<HistoryItem> {
		@Override
		protected void updateItem(HistoryItem item, boolean empty) {
			super.updateItem(item, empty);
			if (item == null) {
				setGraphic(new Text());
			} else {
				Text text;
				if (item.transition == null) {
					// Root item has no transition
					text = new Text("---root---");
				} else {
					// Evaluate the transition so the pretty rep includes
					// argument list and result
					item.transition.evaluate();
					text = new Text(item.transition.getPrettyRep().replace("<--", "←"));
				}

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
	
	private static final Logger logger = LoggerFactory.getLogger(HistoryView.class);

	@FXML private ListView<HistoryItem> lvHistory;
	@FXML private ToggleButton tbReverse;
	@FXML private Button btBack;
	@FXML private Button btForward;

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
			logger.error("loading fxml failed", e);
		}
	}

	@FXML
	public void initialize() {
		this.setMinWidth(100);
		final ChangeListener<Trace> traceChangeListener = (observable, from, to) -> {
			lvHistory.getItems().clear();
			
			if (to != null) {
				int currentPos = to.getCurrent().getIndex();
				addItems(lvHistory,currentPos);
				List<Transition> transitionList = to.getTransitionList();
				for (int i = 0; i < transitionList.size(); i++) {
					HistoryStatus status = getStatus(i,currentPos);
					lvHistory.getItems().add(new HistoryItem(transitionList.get(i), status));
				}
			}
			
			if (tbReverse.isSelected()) {
				Collections.reverse(lvHistory.getItems());
			}
		};
		traceChangeListener.changed(currentTrace, null, currentTrace.get());
		currentTrace.addListener(traceChangeListener);

		btBack.disableProperty().bind(currentTrace.canGoBackProperty().not());
		btForward.disableProperty().bind(currentTrace.canGoForwardProperty().not());

		lvHistory.setCellFactory(item -> new TransitionCell());

		lvHistory.setOnMouseClicked(e -> {
			if (currentTrace.exists()) {
				currentTrace.set(currentTrace.get().gotoPosition(getCurrentIndex()));
			}
		});

		lvHistory.setOnMouseMoved(e -> lvHistory.setCursor(Cursor.HAND));

		tbReverse.setOnAction(e -> Collections.reverse(lvHistory.getItems()));

		btBack.setOnAction(e -> {
			if (currentTrace.exists()) {
				currentTrace.set(currentTrace.back());
			}
		});

		btForward.setOnAction(e -> {
			if (currentTrace.exists()) {
				currentTrace.set(currentTrace.forward());
			}
		});
	}

	private int getCurrentIndex() {
		int currentPos = lvHistory.getSelectionModel().getSelectedIndex();
		int length = lvHistory.getItems().size();
		if (tbReverse.isSelected()) {
			return length - 2 - currentPos;
		} else {
			return currentPos - 1;
		}
	}

	private HistoryStatus getStatus(int i, int currentPos) {
		if (i < currentPos) {
			return HistoryStatus.PAST;
		} else if (i > currentPos) {
			return HistoryStatus.FUTURE;
		} else {
			return HistoryStatus.PRESENT;
		}
	}

	private void addItems(ListView<HistoryItem> lvHistory, int currentPos) {
		lvHistory.getItems().add(new HistoryItem(currentPos == -1 ? HistoryStatus.PRESENT : HistoryStatus.PAST));
	}
}
