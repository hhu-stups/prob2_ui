package de.prob2.ui.animations;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob.exception.CliError;
import de.prob.exception.ProBError;
import de.prob.model.representation.AbstractElement;
import de.prob.model.representation.AbstractModel;
import de.prob.scripting.ModelTranslationError;
import de.prob.statespace.AnimationSelector;
import de.prob.statespace.IAnimationChangeListener;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentTrace;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
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
	private final StageManager stageManager;
	private final CurrentTrace currentTrace;
	private final Locale locale;

	private int currentIndex;
	private int previousSize;

	@Inject
	private AnimationsView(final AnimationSelector animations, final StageManager stageManager,
			CurrentTrace currentTrace, final Locale locale) {
		this.animations = animations;
		this.stageManager = stageManager;
		this.currentTrace = currentTrace;
		this.locale = locale;
		
		this.currentIndex = 0;
		this.previousSize = 0;
		
		this.animations.registerAnimationChangeListener(this);
		this.stageManager.loadFXML(this, "animations_view.fxml");
	}

	@FXML
	public void initialize() {
		machine.setCellValueFactory(new PropertyValueFactory<>("name"));
		lastop.setCellValueFactory(new PropertyValueFactory<>("lastOperation"));
		tracelength.setCellValueFactory(new PropertyValueFactory<>("steps"));
		time.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(
			data.getValue().getTime().format(DateTimeFormatter.ofPattern("HH:mm:ss d MMM uuuu", this.locale))
		).getReadOnlyProperty());
		animationsTable.setRowFactory(tableView -> {
			final TableRow<Animation> row = new TableRow<>();

			final MenuItem removeMenuItem = new MenuItem("Remove Trace");
			removeMenuItem.setOnAction(event -> {
				Animation a = row.getItem();
				animations.removeTrace(a.getTrace());
				animationsTable.getItems().remove(a);
			});
			removeMenuItem.disableProperty().bind(row.emptyProperty());

			final MenuItem removeAllMenuItem = new MenuItem("Remove All Traces");

			removeAllMenuItem.setOnAction(event -> removeAllTraces());

			final MenuItem reloadMenuItem = new MenuItem("Reload");
			reloadMenuItem.setOnAction(event -> {
				try {
					currentTrace.reload(row.getItem().getTrace());
				} catch (CliError | IOException | ModelTranslationError | ProBError e) {
					LOGGER.error("Model reload failed", e);
					stageManager.makeAlert(Alert.AlertType.ERROR, "Failed to reload model:\n" + e).showAndWait();
				}
			});
			reloadMenuItem.disableProperty().bind(row.emptyProperty());

			row.setContextMenu(new ContextMenu(
				removeMenuItem,
				removeAllMenuItem,
				reloadMenuItem
			));

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
	}

	private void removeAllTraces() {
		ObservableList<Animation> animationsList = animationsTable.getItems();
		for (Animation a : animationsList) {
			animations.removeTrace(a.getTrace());
		}
		animationsList.clear();
	}

	private static Animation findExistingAnimation(TableView<Animation> animTable, Animation animation) {
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
	public void traceChange(Trace currentTrace, boolean currentAnimationChanged) {
		List<Trace> traces = animations.getTraces();
		List<Animation> newAnims = new ArrayList<>();
		for (Trace t : traces) {
			AbstractModel model = t.getModel();
			AbstractElement mainComponent = t.getStateSpace().getMainComponent();
			String name = mainComponent == null ? model.getModelFile().getName() : mainComponent.toString();
			Transition op = t.getCurrentTransition();
			String lastOp = op == null ? "" : op.getPrettyRep().replace("<--", "←");
			String steps = Integer.toString(t.getTransitionList().size());
			boolean isCurrent = t.equals(currentTrace);
			boolean isProtected = animations.getProtectedTraces().contains(t.getUUID());
			Animation newAnim = new Animation(name, model, lastOp, steps, t, isCurrent, isProtected);
			Animation oldAnim = findExistingAnimation(animationsTable, newAnim);
			newAnim.setTime(oldAnim == null ? LocalDateTime.now() : oldAnim.getTime());
			newAnims.add(newAnim);
		}
		Platform.runLater(() -> {
			ObservableList<Animation> animationsList = animationsTable.getItems();
			animationsList.clear();
			animationsList.addAll(newAnims);
			if (previousSize < animationsList.size()) {
				currentIndex = animationsList.size() - 1;
			} else if (previousSize > animationsList.size() && currentIndex > 0) {
				currentIndex--;
			}
			animationsTable.getFocusModel().focus(currentIndex);
			previousSize = animationsList.size();
		});
	}

	@Override
	public void animatorStatus(boolean busy) {
		// Not used
	}

	public TableView<Animation> getTable() {
		return animationsTable;
	}
}
