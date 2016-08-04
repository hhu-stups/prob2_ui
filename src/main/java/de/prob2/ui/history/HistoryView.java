package de.prob2.ui.history;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;

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
import javafx.fxml.Initializable;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.AnchorPane;

@Singleton
public class HistoryView extends AnchorPane implements Initializable, IAnimationChangeListener {
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

	public void initialize(URL location, ResourceBundle resources) {

		animations.registerAnimationChangeListener(this);

		lv_history.setCellFactory(new HistoryItemTransformer());

		lv_history.setItems(history);

		lv_history.setOnMouseClicked(e -> {
			animations.traceChange(animations.getCurrentTrace().gotoPosition(getCurrentIndex()));
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
		int index = 0;
		if (rootatbottom) {
			index = length - 2 - currentPos;
		} else {
			index = currentPos - 1;
		}
		return index;

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
			HistoryStatus status = HistoryStatus.PAST;
			if (i == currentPos)
				status = HistoryStatus.PRESENT;
			if (i > currentPos)
				status = HistoryStatus.FUTURE;
			history.add(new HistoryItem(transitionList.get(i), status));
		}

		if (rootatbottom) {
			Collections.reverse(history);
		}
	}

	@Override
	public void animatorStatus(boolean busy) {}
}
