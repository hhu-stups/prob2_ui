package de.prob2.ui.animations;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob.model.representation.AbstractElement;
import de.prob.model.representation.AbstractModel;
import de.prob.statespace.AnimationSelector;
import de.prob.statespace.IAnimationChangeListener;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;
import de.prob2.ui.internal.IComponents;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.internal.UIState;
import de.prob2.ui.prob2fx.CurrentProject;
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
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;

@Singleton
public final class AnimationsView extends AnchorPane implements IAnimationChangeListener, IComponents {
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

	private static final Logger logger = LoggerFactory.getLogger(AnimationsView.class);

	private final AnimationSelector animations;
	private int currentIndex;
	private int previousSize = 0;
	private final Injector injector;

	private CurrentProject currentProject;
	private MachineLoader machineLoader;

	@Inject
	private AnimationsView(final Injector injector, final AnimationSelector animations, final StageManager stageManager,
			final MachineLoader machineLoader, CurrentProject currentProject) {
		this.injector = injector;
		this.animations = animations;
		this.machineLoader = machineLoader;
		this.animations.registerAnimationChangeListener(this);
		this.currentProject = currentProject;
		stageManager.loadFXML(this, "animations_view.fxml");
	}

	@FXML
	public void initialize() {
		machine.setCellValueFactory(new PropertyValueFactory<>("modelName"));
		lastop.setCellValueFactory(new PropertyValueFactory<>("lastOperation"));
		tracelength.setCellValueFactory(new PropertyValueFactory<>("steps"));
		time.setCellValueFactory(new PropertyValueFactory<>("time"));
		animationsTable.setRowFactory(tableView -> {
			final TableRow<Animation> row = new TableRow<>();
			final ContextMenu contextMenu = new ContextMenu();
			final MenuItem removeMenuItem = new MenuItem("Remove Trace");
			removeMenuItem.setOnAction(event -> {
				Animation a = row.getItem();
				animations.removeTrace(a.getTrace());
				animationsTable.getItems().remove(a);
				if (animationsTable.getItems().isEmpty()) {
					currentProject.remove();
				}
			});
			final MenuItem removeAllMenuItem = new MenuItem("Remove All Traces");
			removeAllMenuItem.setOnAction(event -> {
				removeAllTraces();
				currentProject.remove();
			});
			contextMenu.getItems().add(removeMenuItem);
			contextMenu.getItems().add(removeAllMenuItem);
			row.setOnMouseClicked(event -> rowClicked(row, event, contextMenu));
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
				logger.error("loading machine \"" + machine.getName() + "\" failed", e);
				Platform.runLater(() -> {
					Alert alert = new Alert(Alert.AlertType.ERROR,
							"Could not open machine \"" + machine.getName() + "\":\n" + e);
					alert.getDialogPane().getStylesheets().add("prob.css");
					alert.show();
				});
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

	private void rowClicked(TableRow<Animation> row, MouseEvent event, ContextMenu contextMenu) {
		if (!row.isEmpty() && event.getButton() == MouseButton.PRIMARY) {
			currentIndex = row.getIndex();
			Trace trace = row.getItem().getTrace();
			animations.changeCurrentAnimation(trace);
		}
		if (event.getButton() == MouseButton.SECONDARY) {
			if (row.isEmpty()) {
				contextMenu.getItems().get(0).setDisable(true);
			} else {
				contextMenu.getItems().get(0).setDisable(false);
			}
			contextMenu.show(row, event.getScreenX(), event.getScreenY());
		}

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
	
	public double[] getColumnsWidth() {
		return new double[]{machine.getWidth(),lastop.getWidth(),tracelength.getWidth(), time.getWidth()};
	}
	
	public void setColumnsWidth() {
		UIState uiState = injector.getInstance(UIState.class);
		double[] widths = uiState.getAnimationsViewColumnsWidth();
		double width = widths[0] + widths[1] + widths[2] + widths[3];
		List<TableColumn<Animation, ?>> columns = animationsTable.getColumns();
		for (int i = 0; i < columns.size(); i++) {
			animationsTable.resizeColumn(columns.get(i), widths[i] - width/4);
		}
	}
		
	public void setColumnsOrder() {
		UIState uiState = injector.getInstance(UIState.class);
		String[] order = uiState.getAnimationsViewColumnsOrder();
		List<TableColumn<Animation, ?>> newColumns = new ArrayList<>();
		
		for(int i = 0; i < order.length; i++) {
			for(TableColumn<Animation, ?> column : animationsTable.getColumns()) {
				if(column.getText().equals(order[i])) {
					newColumns.add(column);
				}
			}
		}
				
		animationsTable.getColumns().clear();
		animationsTable.getColumns().setAll(newColumns);
	}
	
	public String[] getColumnsOrder() {
		String[] order = new String[4];
		for(int i = 0; i < animationsTable.getColumns().size(); i++) {
			order[i] = animationsTable.getColumns().get(i).getText();
		}
		return order;
	}
}
