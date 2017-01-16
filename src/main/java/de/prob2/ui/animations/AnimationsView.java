package de.prob2.ui.animations;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob.model.representation.AbstractElement;
import de.prob.model.representation.AbstractModel;
import de.prob.scripting.ModelTranslationError;
import de.prob.statespace.AnimationSelector;
import de.prob.statespace.IAnimationChangeListener;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.Machine;
import de.prob2.ui.project.MachineLoader;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.AnchorPane;

@Singleton
public final class AnimationsView extends AnchorPane implements IAnimationChangeListener {
	@FXML
	private TableView<Animation> animationsTable;
	@FXML
	private TableColumn<Animation, String> machine;
	@FXML
	private TableColumn<Animation, String> lastop;
	@FXML
	private TableColumn<Animation, String> tracelength;
	@FXML
	private TableColumn<Animation, String> time;

	private static final Logger LOGGER = LoggerFactory.getLogger(AnimationsView.class);

	private final AnimationSelector animations;
	private final CurrentTrace currentTrace;
	private final StageManager stageManager;

	private int currentIndex;
	private int previousSize = 0;

	private CurrentProject currentProject;
	private MachineLoader machineLoader;

	@Inject
	private AnimationsView(final AnimationSelector animations, final StageManager stageManager,
			final MachineLoader machineLoader, CurrentProject currentProject, CurrentTrace currentTrace) {
		this.animations = animations;
		this.machineLoader = machineLoader;
		this.animations.registerAnimationChangeListener(this);
		this.currentProject = currentProject;
		this.currentTrace = currentTrace;
		this.stageManager = stageManager;
		this.stageManager.loadFXML(this, "animations_view.fxml");
	}

	@FXML
	public void initialize() {
		machine.setCellValueFactory(new PropertyValueFactory<>("modelName"));
		lastop.setCellValueFactory(new PropertyValueFactory<>("lastOperation"));
		tracelength.setCellValueFactory(new PropertyValueFactory<>("steps"));
		time.setCellValueFactory(new PropertyValueFactory<>("time"));
		animationsTable.setRowFactory(tableView -> {
			final TableRow<Animation> row = new TableRow<>();

			final MenuItem removeMenuItem = new MenuItem("Remove Trace");
			removeMenuItem.setOnAction(event -> {
				Animation a = row.getItem();
				animations.removeTrace(a.getTrace());
				animationsTable.getItems().remove(a);
				if (animationsTable.getItems().isEmpty()) {
					currentProject.remove();
				}
			});
			removeMenuItem.disableProperty().bind(row.emptyProperty());

			final MenuItem removeAllMenuItem = new MenuItem("Remove All Traces");

			removeAllMenuItem.setOnAction(event -> {
				removeAllTraces();
				currentProject.remove();
			});

			final MenuItem reloadMenuItem = new MenuItem("Reload");
			reloadMenuItem.setOnAction(event -> {
				try {
					currentTrace.reload(row.getItem().getTrace());
				} catch (IOException | ModelTranslationError e) {
					LOGGER.error("Model reload failed", e);
					stageManager.makeAlert(Alert.AlertType.ERROR, "Failed to reload model:\n" + e).showAndWait();
				}
			});
			reloadMenuItem.disableProperty().bind(row.emptyProperty());

			row.setContextMenu(new ContextMenu(removeMenuItem, removeAllMenuItem, reloadMenuItem));

			row.setOnMouseClicked(event -> {
				if (!row.isEmpty() && event.getButton() == MouseButton.PRIMARY) {
					currentIndex = row.getIndex();
					Trace trace = row.getItem().getTrace();
					animations.changeCurrentAnimation(trace);
				}
			});
			return row;
		});
		this.traceChange(animations.getCurrentTrace(), true);

		currentProject.addListener((observable, from, to) -> {
			if (to != null) {
				removeAllTraces();
				addAll(to.getMachines());
			}
		});
	}

	private void addAll(List<Machine> machines) {
		for (Machine machine : machines) {
			StateSpace stateSpace = machineLoader.load(machine);
			try {
				this.animations.addNewAnimation(new Trace(stateSpace));
			} catch (NullPointerException e) {
				LOGGER.error("loading machine \"" + machine.getName() + "\" failed", e);
				Platform.runLater(() -> stageManager.makeAlert(Alert.AlertType.ERROR, "Could not open machine \"" + machine.getName() + "\":\n" + e).showAndWait());
			}
		}
	}

	private void removeAllTraces() {
		ObservableList<Animation> animationsList = animationsTable.getItems();
		for (Animation a : animationsList) {
			animations.removeTrace(a.getTrace());
		}
		animationsList.clear();
	}

	@Override
	public void traceChange(Trace currentTrace, boolean currentAnimationChanged) {
		List<Trace> traces = animations.getTraces();
		List<Animation> animList = new ArrayList<>();
		for (Trace t : traces) {
			AbstractModel model = t.getModel();
			AbstractElement mainComponent = t.getStateSpace().getMainComponent();
			String modelName = mainComponent == null ? model.getModelFile().getName() : mainComponent.toString();
			Transition op = t.getCurrentTransition();
			String lastOp = op == null ? "" : op.getPrettyRep().replace("<--", "←");
			String steps = Integer.toString(t.getTransitionList().size());
			boolean isCurrent = t.equals(currentTrace);
			boolean isProtected = animations.getProtectedTraces().contains(t.getUUID());
			Animation a = new Animation(modelName, lastOp, steps, t, isCurrent, isProtected);
			Animation aa = contains(animationsTable, a);
			if (aa != null) {
				a.setTime(LocalDateTime.parse(aa.getTime(), DateTimeFormatter.ofPattern("HH:mm:ss d MMM uuuu")));
			} else {
				a.setTime(LocalDateTime.now());
			}
			animList.add(a);
		}
		Platform.runLater(() -> {
			ObservableList<Animation> animationsList = animationsTable.getItems();
			animationsList.clear();
			animationsList.addAll(animList);
			if (previousSize < animationsList.size()) {
				currentIndex = animationsList.size() - 1;
			} else if (previousSize > animationsList.size() && currentIndex > 0) {
				currentIndex--;
			}
			animationsTable.getFocusModel().focus(currentIndex);
			previousSize = animationsList.size();
		});
	}

	private Animation contains(TableView<Animation> animTable, Animation animation) {
		if (animTable != null) {
			for (Animation a : animTable.getItems()) {
				if (a.getTrace().getUUID().equals(animation.getTrace().getUUID())) {
					return a;
				}
			}
		}
		return null;
	}

	@Override
	public void animatorStatus(boolean busy) {
		// Not used
	}

	public ObservableList<TableColumn<Animation, ?>> getColumns() {
		return animationsTable.getColumns();
	}

	public TableView<Animation> getTable() {
		return animationsTable;
	}

}
