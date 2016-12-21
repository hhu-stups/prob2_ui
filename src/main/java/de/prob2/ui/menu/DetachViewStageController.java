package de.prob2.ui.menu;

import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.Injector;

import de.prob2.ui.animations.AnimationsView;
import de.prob2.ui.history.HistoryView;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.internal.UIState;
import de.prob2.ui.modelchecking.ModelcheckingController;
import de.prob2.ui.operations.OperationsView;
import de.prob2.ui.stats.StatsView;

import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TitledPane;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;

public final class DetachViewStageController extends Stage {
	// FIXME all checkboxes selected when reloading detached
	
	@FXML private Button apply;
	@FXML private CheckBox detachOperations;
	@FXML private CheckBox detachHistory;
	@FXML private CheckBox detachModelcheck;
	@FXML private CheckBox detachStats;
	@FXML private CheckBox detachAnimations;
	
	private final Injector injector;
	private final StageManager stageManager;
	private final UIState uiState;
	
	private final Set<Stage> wrapperStages;

	@Inject
	private DetachViewStageController(final Injector injector, final StageManager stageManager, final UIState uiState) {
		this.injector = injector;
		this.stageManager = stageManager;
		this.uiState = uiState;
		
		wrapperStages = new HashSet<>();
		stageManager.loadFXML(this, "detachedPerspectivesChoice.fxml", null);
		this.initModality(Modality.APPLICATION_MODAL);
	}
	
	private <T> T findOfType(final Iterable<? super T> objects, final Class<T> clazz) {
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
	private void apply() {
		apply(MenuController.ApplyDetachedEnum.USER);
	}
	
	/* package */ void apply(MenuController.ApplyDetachedEnum detachedBy) {
		final Parent root = injector.getInstance(MenuController.class).loadPreset("main.fxml");
		final SplitPane pane = findOfType(root.getChildrenUnmodifiable(), SplitPane.class);
		final Accordion accordion = findOfType(pane.getItems(), Accordion.class);
		removeTP(accordion, pane, detachedBy);
		uiState.setGuiState("detached");
		this.hide();
	}
	
	private void removeTP(Accordion accordion, SplitPane pane, MenuController.ApplyDetachedEnum detachedBy) {
		final HashSet<Stage> wrapperStagesCopy = new HashSet<>(wrapperStages);
		wrapperStages.clear();
		for (final Stage stage : wrapperStagesCopy) {
			stage.setScene(null);
			stage.hide();
		}

		for (final Iterator<TitledPane> it = accordion.getPanes().iterator(); it.hasNext();) {
			final TitledPane tp = it.next();
			if (removable(tp, detachedBy)) {
				it.remove();
				transferToNewWindow((Parent)tp.getContent(), tp.getText());
			}
		}
		if (accordion.getPanes().isEmpty()) {
			pane.getItems().remove(accordion);
			pane.setDividerPositions(0);
			pane.lookupAll(".split-pane-divider").forEach(div -> div.setMouseTransparent(true));
		}
	}
	
	private boolean removable(TitledPane tp, MenuController.ApplyDetachedEnum detachedBy) {
		return	(removablePane(tp, detachOperations, detachedBy) && tp.getContent() instanceof OperationsView) ||
				(removablePane(tp, detachHistory, detachedBy) && tp.getContent() instanceof HistoryView) ||
				(removablePane(tp, detachModelcheck, detachedBy) && tp.getContent() instanceof ModelcheckingController) ||
				(removablePane(tp, detachStats, detachedBy) && tp.getContent() instanceof StatsView) ||
				(removablePane(tp, detachAnimations, detachedBy) && tp.getContent() instanceof AnimationsView);
	}
	
	private boolean removablePane(TitledPane tp, CheckBox detached, MenuController.ApplyDetachedEnum detachedBy) {
		boolean condition = detached.isSelected();
		if(detachedBy == MenuController.ApplyDetachedEnum.JSON) {
			condition = uiState.getSavedStageBoxes().containsKey(tp.getText());
			if(condition) {
				detached.setSelected(true);
			}
		}
		return condition;
	}

	private void transferToNewWindow(Parent node, String title) {
		Stage stage = stageManager.makeStage(new Scene(node), this.getClass().getName() + " detached " + node.getClass().getName());
		wrapperStages.add(stage);
		stage.setTitle(title);
		stage.setOnHidden(e -> {
			if (node instanceof OperationsView) {
				detachOperations.setSelected(false);
			} else if (node instanceof HistoryView) {
				detachHistory.setSelected(false);
			} else if (node instanceof ModelcheckingController) {
				detachModelcheck.setSelected(false);
			} else if (node instanceof StatsView) {
				detachStats.setSelected(false);
			} else if (node instanceof AnimationsView) {
				detachAnimations.setSelected(false);
			}
			this.apply();
		});
		
		// Default bounds, replaced by saved ones from the config when show() is called
		stage.setWidth(200);
		stage.setHeight(100);
		stage.setX(Screen.getPrimary().getVisualBounds().getWidth()-stage.getWidth()/2);
		stage.setY(Screen.getPrimary().getVisualBounds().getHeight()-stage.getHeight()/2);
		stage.show();
	}
}
