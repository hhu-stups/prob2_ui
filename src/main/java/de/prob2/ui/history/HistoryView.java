package de.prob2.ui.history;

import static de.prob2.ui.history.HistoryStatus.FUTURE;
import static de.prob2.ui.history.HistoryStatus.PAST;
import static de.prob2.ui.history.HistoryStatus.PRESENT;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;

import de.prob.statespace.AnimationSelector;
import de.prob.statespace.IAnimationChangeListener;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;
import de.prob2.ui.events.TraceChangeDirection;
import de.prob2.ui.events.TraceChangeEvent;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleButton;

public class HistoryView extends TitledPane implements Initializable, IAnimationChangeListener {

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

	private EventBus bus;
	
	@Inject
	public HistoryView(FXMLLoader loader, AnimationSelector animations, EventBus bus) {
		this.animations = animations;
		animations.registerAnimationChangeListener(this);
		
		this.bus = bus;
		try {
			loader.setLocation(getClass().getResource("history_view.fxml"));
			loader.setRoot(this);
			loader.setController(this);
			loader.load();
		} catch (IOException e) {
			e.printStackTrace();
		}
		bus.register(this);
	}

	public void initialize(URL location, ResourceBundle resources) {
		
		animations.registerAnimationChangeListener(this);

		lv_history.setCellFactory(new HistoryItemTransformer());
				
		lv_history.setItems(history);
		
		
		lv_history.setOnMouseClicked(e -> {
			bus.post(new TraceChangeEvent(getCurrentIndex()));
		});
		

		lv_history.setOnMouseMoved(e -> {
			lv_history.setCursor(Cursor.HAND);
		});

		tb_reverse.setOnAction(e -> {
			Collections.reverse(history);
			rootatbottom = !rootatbottom;
		});

		btprevious.setOnAction(e -> {
			bus.post(new TraceChangeEvent(TraceChangeDirection.BACK));
		});

		btforward.setOnAction(e -> {
			bus.post(new TraceChangeEvent(TraceChangeDirection.FORWARD));
		});


	}
	
	@Subscribe
	public void changeTracePosition(TraceChangeEvent event) {
		switch(event.getDirection()) {
			case BACK:
				animations.traceChange(animations.getCurrentTrace().back());
				break;
			case FORWARD:
				animations.traceChange(animations.getCurrentTrace().forward());
				break;
			default:
				animations.traceChange(animations.getCurrentTrace().gotoPosition(event.getIndex()));
				break;
		}
		
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
			history.add(new HistoryItem(PRESENT));
		} else {
			history.add(new HistoryItem(PAST));
		}
		
		for (int i = 0; i < transitionList.size(); i++) {
			HistoryStatus status = PAST;
			if (i == currentPos)
				status = PRESENT;
			if (i > currentPos)
				status = FUTURE;
			history.add(new HistoryItem(transitionList.get(i), status));
		}

		 if (rootatbottom) {
			 Collections.reverse(history);
		 }
	}

	@Override
	public void animatorStatus(boolean busy) {
		// TODO Auto-generated method stub

	}

}
