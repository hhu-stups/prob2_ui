package de.prob2.ui.animations;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob.model.representation.AbstractElement;
import de.prob.model.representation.AbstractModel;
import de.prob.statespace.AnimationSelector;
import de.prob.statespace.IAnimationChangeListener;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;

@Singleton
public class AnimationsView extends AnchorPane implements IAnimationChangeListener {
	@FXML
	private TableView<Animation> animationsTable;
	@FXML
	private TableColumn<Animation, String> machine;
	@FXML
	private TableColumn<Animation, String> lastop;
	@FXML
	private TableColumn<Animation, String> tracelength;

	private AnimationSelector animations;
	private Map<UUID, Animation> animationsList = new HashMap<UUID, Animation>();

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
			e.printStackTrace();
		}
	}

	@FXML
	public void initialize() {
		machine.setCellValueFactory(new PropertyValueFactory<>("modelName"));
		lastop.setCellValueFactory(new PropertyValueFactory<>("lastOperation"));
		tracelength.setCellValueFactory(new PropertyValueFactory<>("steps"));
	}

	@Override
	public void traceChange(Trace currentTrace, boolean currentAnimationChanged) {
		List<Trace> traces = animations.getTraces();
		// Object[] result = new Object[traces.size()];
		// int ctr = 0;
		for (Trace t : traces) {
			AbstractModel model = t.getModel();
			AbstractElement mainComponent = t.getStateSpace().getMainComponent();
			String modelName = mainComponent != null ? mainComponent.toString() : model.getModelFile().getName();
			Transition op = t.getCurrentTransition();
			String lastOp = op != null ? op.getPrettyRep() : "";
			String steps = t.getTransitionList().size() + "";
			if(animationsList.containsKey(t.getUUID())) {
				Animation a = animationsList.get(t.getUUID());
				Platform.runLater(() -> {
					a.setLastOperation(lastOp);
					a.setSteps(steps);
					animationsTable.refresh();
				});
			} else {
				Animation a = new Animation(modelName, lastOp, steps);
				animationsList.put(t.getUUID(), a);
				Platform.runLater(() -> animationsTable.getItems().add(a));
			}
			// String isCurrent = t.equals(currentTrace) + "";
			// boolean isProtected =
			// animations.getProtectedTraces().contains(t.getUUID());
		}
	}

	@Override
	public void animatorStatus(boolean busy) {
	}

}
