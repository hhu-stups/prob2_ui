package de.prob2.ui.animations;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.prob.model.representation.AbstractElement;
import de.prob.model.representation.AbstractModel;
import de.prob.statespace.AnimationSelector;
import de.prob.statespace.IAnimationChangeListener;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.AnchorPane;

//@Singleton
public class AnimationsView extends AnchorPane implements IAnimationChangeListener {
	@FXML
	private TableView<Animation> animationsTable;
	@FXML
	private TableColumn<Animation, String> machine;
	@FXML
	private TableColumn<Animation, String> lastop;
	@FXML
	private TableColumn<Animation, String> tracelength;

	private final AnimationSelector animations;
	private int currentIndex;
	private int previousSize = 0;

	private Logger logger = LoggerFactory.getLogger(AnimationsView.class);

	@Inject
	private AnimationsView(final AnimationSelector animations, final FXMLLoader loader) {
		this.animations = animations;
		this.animations.registerAnimationChangeListener(this);
		try {
			loader.setLocation(getClass().getResource("animations_view.fxml"));
			loader.setRoot(this);
			loader.setController(this);
			loader.load();
		} catch (IOException e) {
			logger.error("loading fxml failed", e);
		}
	}

	@FXML
	public void initialize() {
		machine.setCellValueFactory(new PropertyValueFactory<>("modelName"));
		lastop.setCellValueFactory(new PropertyValueFactory<>("lastOperation"));
		tracelength.setCellValueFactory(new PropertyValueFactory<>("steps"));
		animationsTable.setRowFactory(tableView -> {
			final TableRow<Animation> row = new TableRow<>();
			final ContextMenu contextMenu = new ContextMenu();
			final MenuItem removeMenuItem = new MenuItem("Remove Trace");
			removeMenuItem.setOnAction(event -> {
				Animation a = row.getItem();
				animations.removeTrace(a.getTrace());
				animationsTable.getItems().remove(a);
			});
			contextMenu.getItems().add(removeMenuItem);
			row.setOnMouseClicked(event -> {
				if (!row.isEmpty()) {
					if (event.getButton().equals(MouseButton.PRIMARY)) {
						currentIndex = row.getIndex();
						Trace trace = row.getItem().getTrace();
						animations.changeCurrentAnimation(trace);
					}
					if (event.getButton().equals(MouseButton.SECONDARY)) {
						contextMenu.show(row, event.getScreenX(), event.getScreenY());
					}
				}
			});
			return row;
		});
	}

	@Override
	public void traceChange(Trace currentTrace, boolean currentAnimationChanged) {
		List<Trace> traces = animations.getTraces();
		List<Animation> animList = new ArrayList<>();
		for (Trace t : traces) {
			AbstractModel model = t.getModel();
			AbstractElement mainComponent = t.getStateSpace().getMainComponent();
			String modelName = mainComponent != null ? mainComponent.toString() : model.getModelFile().getName();
			Transition op = t.getCurrentTransition();
			String lastOp = op != null ? op.getPrettyRep() : "";
			String steps = t.getTransitionList().size() + "";
			boolean isCurrent = t.equals(currentTrace);
			boolean isProtected = animations.getProtectedTraces().contains(t.getUUID());
			Animation a = new Animation(modelName, lastOp, steps, t, isCurrent, isProtected);
			animList.add(a);
		}
		Platform.runLater(() -> {
			ObservableList<Animation> animationsList = animationsTable.getItems();
			animationsList.clear();
			animationsList.addAll(animList);
			if (previousSize < animationsList.size())
				currentIndex = animationsList.size() - 1;
			else if (previousSize > animationsList.size() && currentIndex > 0)
				currentIndex--;
			animationsTable.getFocusModel().focus(currentIndex);
			previousSize = animationsList.size();
		});
	}

	@Override
	public void animatorStatus(boolean busy) {
	}

}
