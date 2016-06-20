package de.prob2.ui.history;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;

import com.google.inject.Inject;

import static de.prob2.ui.history.HistoryStatus.*;

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
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.stage.Stage;

public class HistoryView extends TitledPane implements Initializable, IAnimationChangeListener {

	@FXML
	private ListView<HistoryItem> lv_history;

	@FXML
	private ToggleButton tb_reverse;

	@FXML
	private Button btprevious;

	@FXML
	private Button btforward;

	@FXML
	private Button btshowgraph;

	private boolean rootatbottom = true;

	private ObservableList<HistoryItem> history = FXCollections.observableArrayList();

	private AnimationSelector animations;

	@Inject
	public HistoryView(FXMLLoader loader, AnimationSelector animations) {
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
			Collections.reverse(lv_history.getItems());
			rootatbottom = !rootatbottom;
			animations.traceChange(animations.getCurrentTrace().gotoPosition(getCurrentIndex()));
		});

		btprevious.setOnAction(e -> {
			animations.traceChange(animations.getCurrentTrace().back());
		});

		btforward.setOnAction(e -> {
			animations.traceChange(animations.getCurrentTrace().forward());
		});

		btshowgraph.setOnAction(e -> {
			String url = getClass().getResource("/glyphicons_free/glyphicons/png/glyphicons-9-film.png").toString();
			ScrollPane pane = new ScrollPane();
			Stage stage = new Stage();
			ImageView graph = new ImageView(new Image(url));
			graph.setFitHeight(1000);
			graph.setFitWidth(1000);
			graph.setOnMouseClicked(graphe -> {
				if (graphe.getButton() == MouseButton.PRIMARY) {
					graph.setFitHeight(graph.getFitHeight() * 2);
					graph.setFitWidth(graph.getFitWidth() * 2);
				} else if (graphe.getButton() == MouseButton.SECONDARY) {
					graph.setFitHeight(graph.getFitHeight() * 0.5);
					graph.setFitWidth(graph.getFitWidth() * 0.5);
				}
				pane.setContent(graph);
			});

			pane.setContent(graph);
			stage.setTitle("Dotty");
			Scene scene = new Scene(pane, 800, 600);
			stage.setScene(scene);
			stage.show();
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
		// if (lv_history == null) {
		// return;
		// }
		// lv_history.getItems().clear();

		history.clear();
		int currentPos = currentTrace.getCurrent().getIndex();

		List<Transition> transitionList = currentTrace.getTransitionList();
		for (int i = 0; i < transitionList.size(); i++) {
			HistoryStatus status = PAST;
			if (i == currentPos)
				status = PRESENT;
			if (i < currentPos)
				status = FUTURE;
			history.add(new HistoryItem(transitionList.get(i), status));
		}

		// List<Transition> opList = currentTrace.getTransitionList();
		// int startpos = 0;
		// int endpos = opList.size();
		//
		// lv_history.getItems().add("---root---");
		// currentTrace.getStateSpace().evaluateTransitions(opList.subList(startpos,
		// endpos), FormulaExpand.truncate);
		// //
		// for (int i = startpos; i < endpos; i++) {
		// Text rep = new Text(opList.get(i).getPrettyRep());
		// if (i > currentPos) {
		// rep.setFont(Font.font("ARIAL", FontPosture.ITALIC, 12));
		// rep.setFill(Color.GRAY);
		// } else if (i == currentPos) {
		// rep.setFont(Font.font("ARIAL", FontWeight.BOLD, 12));
		// }
		// lv_history.getItems().add(rep);
		// }
		//
		// if (rootatbottom) {
		// Collections.reverse(lv_history.getItems());
		// }
	}

	@Override
	public void animatorStatus(boolean busy) {
		// TODO Auto-generated method stub

	}

}
