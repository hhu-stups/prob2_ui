package de.prob2.ui.history;

import java.util.Collections;
import java.util.List;

import com.google.inject.Inject;

import de.prob.statespace.Trace;
import de.prob.statespace.Transition;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentTrace;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.Text;

public final class HistoryView extends AnchorPane {
	private static class TransitionCell extends ListCell<HistoryItem> {
		@Override
		protected void updateItem(HistoryItem item, boolean empty) {
			super.updateItem(item, empty);
			if (item == null) {
				setGraphic(new Text());
			} else {
				final Text text = new Text(transitionToString(item.transition));

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
	
	@FXML private ListView<HistoryItem> lvHistory;
	@FXML private ToggleButton tbReverse;
	@FXML private Button btBack;
	@FXML private Button btForward;

	private final CurrentTrace currentTrace;

	@Inject
	private HistoryView(StageManager stageManager, CurrentTrace currentTrace) {
		this.currentTrace = currentTrace;
		stageManager.loadFXML(this, "history_view.fxml");
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
	
	public static String transitionToString(final Transition transition) {
		if (transition == null) {
			// Root item has no transition
			return "---root---";
		} else {
			// Evaluate the transition so the pretty rep includes argument list and result
			transition.evaluate();
			return transition.getPrettyRep().replace("<--", "←");
		}
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
