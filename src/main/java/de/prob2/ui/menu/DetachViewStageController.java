package de.prob2.ui.menu;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob2.ui.MainController;
import de.prob2.ui.animation.AnimationView;
import de.prob2.ui.history.HistoryView;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.operations.OperationsView;
import de.prob2.ui.persistence.UIState;
import de.prob2.ui.project.ProjectView;
import de.prob2.ui.stats.StatsView;
import de.prob2.ui.verifications.VerificationsView;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public final class DetachViewStageController extends Stage {
	private static final Logger LOGGER = LoggerFactory.getLogger(DetachViewStageController.class);
	
	@FXML private Button apply;
	@FXML private CheckBox detachOperations;
	@FXML private CheckBox detachAnimation;
	@FXML private CheckBox detachHistory;
	@FXML private CheckBox detachVerifications;
	@FXML private CheckBox detachStats;
	@FXML private CheckBox detachProject;
	
	private final Injector injector;
	private final StageManager stageManager;
	private final UIState uiState;
	
	private final Map<Class<?>, CheckBox> checkBoxMap;
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
	
	@FXML
	public void initialize() {
		checkBoxMap.put(OperationsView.class, detachOperations);
		checkBoxMap.put(AnimationView.class, detachAnimation);
		checkBoxMap.put(HistoryView.class, detachHistory);
		checkBoxMap.put(VerificationsView.class, detachVerifications);
		checkBoxMap.put(StatsView.class, detachStats);
		checkBoxMap.put(ProjectView.class, detachProject);
	}
	
	public void selectForDetach(final String name) {
		final Class<?> clazz;
		try {
			clazz = Class.forName(name);
		} catch (ClassNotFoundException e) {
			LOGGER.warn("Not a valid class name, cannot select for detaching", e);
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
	private void apply() {
		doDetaching();
		this.setOnCloseRequest(e -> {
			if (!wrapperStages.isEmpty()) {
				for (Stage stage : wrapperStages) {
					List<Node> child = stage.getScene().getRoot().getChildrenUnmodifiable();
					if (!child.isEmpty()) {
						checkBoxMap.get(child.get(0).getClass()).setSelected(true);
					}
				}
			}
		});
		this.hide();
	}

	public void doDetaching() {
		uiState.updateSavedStageBoxes();
		wrapperStages.forEach(Window::hide);
		injector.getInstance(MainController.class).getAccordions().forEach(this::detachTitledPanes);
		updateWrapperStages();
	}
	
	private void updateWrapperStages() {
		for (Stage stage : wrapperStages) {
			List<Node> child = stage.getScene().getRoot().getChildrenUnmodifiable();
			if (!child.isEmpty() && !checkBoxMap.get(child.get(0).getClass()).isSelected()) {
				stage.hide();
				Platform.runLater(() -> wrapperStages.remove(stage));
			}
		}
	}

	public void attachAllViews() {
		for (CheckBox cb : checkBoxMap.values()) {
			cb.setSelected(false);
		}
		this.apply();
	}
	
	private void detachTitledPanes(Accordion accordion) {
		for (final Iterator<TitledPane> it = accordion.getPanes().iterator(); it.hasNext();) {
			final TitledPane tp = it.next();
			if (checkBoxMap.get(tp.getContent().getClass()).isSelected()) {
				it.remove();
				Platform.runLater(() -> transferToNewWindow(tp, tp.getText(), accordion));
			}
		}
		if (accordion.getPanes().isEmpty()) {
			accordion.setVisible(false);
			accordion.setMaxWidth(0);
			accordion.setMaxHeight(0);
		}
	}
	
	private void transferToNewWindow(TitledPane tp, String title, Accordion accordion) {
		Node node = tp.getContent();
		tp.setContent(null);
		Stage stage = stageManager.makeStage(new Scene(new StackPane()), this.getClass().getName() + " DETACHED " + node.getClass().getName());
		((StackPane) stage.getScene().getRoot()).getChildren().add(node);
		node.setVisible(true);
		wrapperStages.add(stage);
		stage.setTitle(title);
		stage.setOnCloseRequest(e -> {
			checkBoxMap.get(node.getClass()).setSelected(false);
			accordion.setVisible(true);
			accordion.setMaxWidth(Double.POSITIVE_INFINITY);
			accordion.setMaxHeight(Double.POSITIVE_INFINITY);
			if (accordion.getExpandedPane()!=null) {
				accordion.getExpandedPane().setExpanded(false);
			}
			accordion.setExpandedPane(tp);
			tp.setContent(node);
			accordion.getPanes().add(tp);
			wrapperStages.remove(stage);
		});
		// Default bounds, replaced by saved ones from the config when show() is called
		stage.setWidth(200);
		stage.setHeight(100);
		stage.setX((Screen.getPrimary().getVisualBounds().getWidth()-stage.getWidth())/2);
		stage.setY((Screen.getPrimary().getVisualBounds().getHeight()-stage.getHeight())/2);
		stage.show();
	}
}
