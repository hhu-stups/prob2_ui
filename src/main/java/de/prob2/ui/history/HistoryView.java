package de.prob2.ui.history;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.prob.statespace.AnimationSelector;
import de.prob.statespace.IAnimationChangeListener;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
public class HistoryView extends AnchorPane implements IAnimationChangeListener {
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

	private boolean rootatbottom = true;

	private ObservableList<HistoryItem> history = FXCollections.observableArrayList();

	private AnimationSelector animations;

	@Inject
	private HistoryView(FXMLLoader loader, AnimationSelector animations) {
		this.animations = animations;
		animations.registerAnimationChangeListener(this);

		try {
			loader.setLocation(getClass().getResource("history_view.fxml"));
			loader.setRoot(this);
			loader.setController(this);
			loader.load();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@FXML
	public void initialize() {

		animations.registerAnimationChangeListener(this);

		lv_history.setCellFactory(item -> new TransitionCell());

		lv_history.setItems(history);

		lv_history.setOnMouseClicked(e -> {
			if (animations.getCurrentTrace() != null) {
				animations.traceChange(animations.getCurrentTrace().gotoPosition(getCurrentIndex()));
			}
		});

		lv_history.setOnMouseMoved(e -> {
			lv_history.setCursor(Cursor.HAND);
		});

		tb_reverse.setOnAction(e -> {
			Collections.reverse(history);
			rootatbottom = !rootatbottom;
		});

		btprevious.setOnAction(e -> {
			if (animations.getCurrentTrace() != null) {
				animations.traceChange(animations.getCurrentTrace().back());
			}
		});

		btforward.setOnAction(e -> {
			if (animations.getCurrentTrace() != null) {
				animations.traceChange(animations.getCurrentTrace().forward());
			}
		});

	}

	private int getCurrentIndex() {
		int currentPos = lv_history.getSelectionModel().getSelectedIndex();
		int length = lv_history.getItems().size();
		if (rootatbottom) {
			return length - 2 - currentPos;
		} else {
			return currentPos - 1;
		}

	}

	@Override
	public void traceChange(Trace currentTrace, boolean currentAnimationChanged) {
		history.clear();
		int currentPos = currentTrace.getCurrent().getIndex();
		List<Transition> transitionList = currentTrace.getTransitionList();
		
		if (currentPos == -1) {
			history.add(new HistoryItem(HistoryStatus.PRESENT));
		} else {
			history.add(new HistoryItem(HistoryStatus.PAST));
		}
		
		for (int i = 0; i < transitionList.size(); i++) {
			HistoryStatus status;
			if (i < currentPos) {
				status = HistoryStatus.PAST;
			} else if (i > currentPos) {
				status = HistoryStatus.FUTURE;
			} else {
				status = HistoryStatus.PRESENT;
			}
			history.add(new HistoryItem(transitionList.get(i), status));
		}
		
		if (rootatbottom) {
			Collections.reverse(history);
		}
		btprevious.setDisable(!currentTrace.canGoBack());
		btforward.setDisable(!currentTrace.canGoForward());
	}

	@Override
	public void animatorStatus(boolean busy) {
	}
}
