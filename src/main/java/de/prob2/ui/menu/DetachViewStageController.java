package de.prob2.ui.menu;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import de.prob2.ui.project.ProjectView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob2.ui.animations.AnimationsView;
import de.prob2.ui.history.HistoryView;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.modelchecking.ModelcheckingController;
import de.prob2.ui.operations.OperationsView;
import de.prob2.ui.persistence.UIState;
import de.prob2.ui.stats.StatsView;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;

@Singleton
public final class DetachViewStageController extends Stage {
	private static final Logger LOGGER = LoggerFactory.getLogger(DetachViewStageController.class);
	
	@FXML private Button apply;
	@FXML private CheckBox detachOperations;
	@FXML private CheckBox detachHistory;
	@FXML private CheckBox detachModelcheck;
	@FXML private CheckBox detachStats;
	@FXML private CheckBox detachProjects;
	@FXML private CheckBox detachAnimations;
	
	private final Injector injector;
	private final StageManager stageManager;
	private final UIState uiState;
	
	private final Map<Class<? extends Parent>, CheckBox> checkBoxMap;
	private final Set<Stage> wrapperStages;
	
	@Inject
	private DetachViewStageController(final Injector injector, final StageManager stageManager, final UIState uiState) {
		this.injector = injector;
		this.stageManager = stageManager;
		this.uiState = uiState;
		
		checkBoxMap = new HashMap<>();
		wrapperStages = new HashSet<>();
		stageManager.loadFXML(this, "detachedPerspectivesChoice.fxml", null);
		this.initModality(Modality.APPLICATION_MODAL);
	}
	
	private static <T> T findOfType(final Iterable<? super T> objects, final Class<T> clazz) {
		for (final Object o : objects) {
			try {
				return clazz.cast(o);
			} catch (ClassCastException ignored) { // NOSONAR
				// Object doesn't have the type that we want, try the next one
			}
		}
		throw new NoSuchElementException(String.format("No %s object found in %s", clazz, objects));
	}
	
	@FXML
	public void initialize() {
		checkBoxMap.put(OperationsView.class, detachOperations);
		checkBoxMap.put(HistoryView.class, detachHistory);
		checkBoxMap.put(ModelcheckingController.class, detachModelcheck);
		checkBoxMap.put(StatsView.class, detachStats);
		checkBoxMap.put(ProjectView.class, detachProjects);
		checkBoxMap.put(AnimationsView.class, detachAnimations);
	}
	
	public void selectForDetach(final String name) {
		final Class<? extends Parent> clazz;
		try {
			clazz = Class.forName(name).asSubclass(Parent.class);
		} catch (ClassNotFoundException e) {
			LOGGER.warn("Not a valid class name, cannot select for detaching", e);
			return;
		} catch (ClassCastException e) {
			LOGGER.warn("Not a subclass of Parent, cannot select for detaching", e);
			return;
		}
		
		final CheckBox checkBox = checkBoxMap.get(clazz);
		if (checkBox == null) {
			LOGGER.warn("No check box found for {}, cannot select for detaching", clazz);
			return;
		}
		checkBox.setSelected(true);
	}

	@FXML
	public void apply() {
		final Parent root = injector.getInstance(MenuController.class).loadPreset("main.fxml");
		final SplitPane pane = findOfType(root.getChildrenUnmodifiable(), SplitPane.class);
		final Accordion accordion = findOfType(pane.getItems(), Accordion.class);
		removeTP(accordion, pane);
		uiState.setGuiState("detached");
		this.hide();
	}
	
	private void removeTP(Accordion accordion, SplitPane pane) {
		uiState.updateSavedStageBoxes();
		for (Stage stage : wrapperStages){
			Platform.runLater(stage::hide);
		}
		for (final Iterator<TitledPane> it = accordion.getPanes().iterator(); it.hasNext();) {
			final TitledPane tp = it.next();
			if (checkBoxMap.get(tp.getContent().getClass()).isSelected()) {
				it.remove();
				Platform.runLater(() -> transferToNewWindow(tp, tp.getText(), accordion, pane));
			}
		}
		if (accordion.getPanes().isEmpty()) {
			accordion.setVisible(false);
			accordion.setMaxWidth(0);
			pane.setDividerPositions(0);
			pane.lookupAll(".split-pane-divider").forEach(div -> div.setMouseTransparent(true));
		}
	}
	
	private void transferToNewWindow(TitledPane tp, String title, Accordion accordion, SplitPane pane) {
		Parent node = (Parent) tp.getContent();
		tp.setContent(null);
		Stage stage = stageManager.makeStage(new Scene(new StackPane()), this.getClass().getName() + " detached " + node.getClass().getName());
		((StackPane) stage.getScene().getRoot()).getChildren().add(node);
		node.setVisible(true);
		wrapperStages.add(stage);
		stage.setTitle(title);
		stage.setOnCloseRequest(e -> {
			checkBoxMap.get(node.getClass()).setSelected(false);
			accordion.setVisible(true);
			accordion.setMaxWidth(Double.POSITIVE_INFINITY);
			pane.setDividerPositions(uiState.getHorizontalDividerPositions());
			pane.lookupAll(".split-pane-divider").forEach(div -> div.setMouseTransparent(false));
			tp.setContent(node);
			accordion.getPanes().add(tp);
			wrapperStages.remove(stage);
		});
		// Default bounds, replaced by saved ones from the config when show() is called
		stage.setWidth(200);
		stage.setHeight(100);
		stage.setX(Screen.getPrimary().getVisualBounds().getWidth()-stage.getWidth()/2);
		stage.setY(Screen.getPrimary().getVisualBounds().getHeight()-stage.getHeight()/2);
		stage.show();
	}
}
