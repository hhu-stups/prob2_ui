package de.prob2.ui.menu;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import de.prob2.ui.MainController;
import de.prob2.ui.animation.AnimationView;
import de.prob2.ui.consoles.b.BConsoleView;
import de.prob2.ui.history.HistoryView;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.operations.OperationsView;
import de.prob2.ui.persistence.UIState;
import de.prob2.ui.project.ProjectView;
import de.prob2.ui.stats.StatsView;
import de.prob2.ui.verifications.VerificationsView;
import de.prob2.ui.visualisation.VisualisationsView;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


@Singleton
public final class DetachViewStageController extends Stage {
	private static final Logger LOGGER = LoggerFactory.getLogger(DetachViewStageController.class);
	
	public static final String PERSISTENCE_ID_PREFIX = DetachViewStageController.class.getName() + " DETACHED ";
	
	@FXML private Button apply;
	@FXML private CheckBox detachOperations;
	@FXML private CheckBox detachAnimation;
	@FXML private CheckBox detachHistory;
	@FXML private CheckBox detachVerifications;
	@FXML private CheckBox detachStats;
	@FXML private CheckBox detachProject;
	@FXML private CheckBox detachConsole;
	@FXML private CheckBox detachVisualisations;

	private final Injector injector;
	private final StageManager stageManager;
	private final UIState uiState;
	
	private final Map<Class<?>, CheckBox> checkBoxMap;
	private final Set<DetachedViewStage> wrapperStages;
	
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
		checkBoxMap.put(BConsoleView.class, detachConsole);
		checkBoxMap.put(VisualisationsView.class, detachVisualisations);
		this.setOnCloseRequest(e -> {
			for (DetachedViewStage stage : wrapperStages) {
				checkBoxMap.get(stage.getDetachedView().getClass()).setSelected(true);
			}
		});
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
		this.hide();
	}

	public void doDetaching() {
		uiState.updateSavedStageBoxes();
		wrapperStages.forEach(Window::hide);
		wrapperStages.clear();
		injector.getInstance(MainController.class).getAccordions().forEach(this::detachTitledPanes);
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
				transferToNewWindow(tp, accordion);
			}
		}
		if (accordion.getPanes().isEmpty()) {
			accordion.setVisible(false);
			accordion.setMaxWidth(0);
			accordion.setMaxHeight(0);
		}
	}
	
	private void transferToNewWindow(TitledPane tp, Accordion accordion) {
		Node node = tp.getContent();
		// Remove the detached view from the TitledPane.
		// A dummy node is used instead of null to prevent NullPointerExceptions from internal JavaFX code (mostly related to TitledPane/Accordion animations).
		tp.setContent(new Label("View is detached\n(this label should be invisible)"));
		DetachedViewStage stage = new DetachedViewStage(stageManager, node, tp, accordion);
		node.setVisible(true);
		wrapperStages.add(stage);
		stage.setOnCloseRequest(e -> {
			checkBoxMap.get(node.getClass()).setSelected(false);
			wrapperStages.remove(stage);
		});
		stage.show();
		stage.toFront();
	}
}
