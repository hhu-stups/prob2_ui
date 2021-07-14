package de.prob2.ui.visb;

import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.inject.Injector;

import de.prob.animator.domainobjects.VisBEvent;
import de.prob.animator.domainobjects.VisBHover;
import de.prob.animator.domainobjects.VisBItem;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.visb.ui.ListViewEvent;
import de.prob2.ui.visb.ui.ListViewItem;
import de.prob2.ui.visb.visbobjects.VisBVisualisation;

import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.stage.Stage;

@Singleton
public class VisBDebugStage extends Stage {

	private final StageManager stageManager;

	private final CurrentTrace currentTrace;

	private final CurrentProject currentProject;

	private final ResourceBundle bundle;

	private final VisBController visBController;

	private final Injector injector;

	@FXML
	private ListView<VisBItem> visBItems;
	@FXML
	private ListView<VisBEvent> visBEvents;

	private final Map<String, VisBEvent> eventsById;

	@Inject
	public VisBDebugStage(final StageManager stageManager, final CurrentTrace currentTrace, final CurrentProject currentProject, final ResourceBundle bundle, final VisBController visBController, final Injector injector) {
		super();
		this.stageManager = stageManager;
		this.currentTrace = currentTrace;
		this.currentProject = currentProject;
		this.bundle = bundle;
		this.visBController = visBController;
		this.injector = injector;
		this.eventsById = new HashMap<>();
		this.stageManager.loadFXML(this, "visb_debug_stage.fxml");
	}

	@FXML
	public void initialize() {
		visBController.visBVisualisationProperty().addListener((o, from, to) -> this.initialiseListViews(to));
		
		ChangeListener<VisBItem> listener = (observable, from, to) -> {
			if(from != null) {
				removeHighlighting(from);
			}
			if(to != null) {
				applyHighlighting(to);
			}
		};
		this.visBItems.setCellFactory(lv -> new ListViewItem(stageManager, bundle, injector, eventsById, visBController.getAttributeValues()));
		this.visBEvents.setCellFactory(lv -> new ListViewEvent(stageManager, bundle, injector));
		this.currentTrace.addListener((observable, from, to) -> refresh());
		this.currentProject.currentMachineProperty().addListener((observable, from, to) -> refresh());
		this.visBItems.getSelectionModel().selectedItemProperty().addListener(listener);
		this.setOnCloseRequest(e -> this.visBItems.getSelectionModel().clearSelection());
	}

	private void removeHighlighting(VisBItem item) {
		String id = item.getId();
		if(eventsById.containsKey(id)) {
			for (VisBHover hover : eventsById.get(id).getHovers()) {
				injector.getInstance(VisBStage.class).changeAttribute(hover.getHoverID(), hover.getHoverAttr(), hover.getHoverLeaveVal());
			}
		}
	}

	private void applyHighlighting(VisBItem item) {
		String id = item.getId();
		if(eventsById.containsKey(id)) {
			for (VisBHover hover : eventsById.get(id).getHovers()) {
				injector.getInstance(VisBStage.class).changeAttribute(hover.getHoverID(), hover.getHoverAttr(), hover.getHoverEnterVal());
			}
		}
	}

	/**
	 * After loading the JSON/ VisB file and preparing it in the {@link VisBController} the ListViews are initialised.
	 * @param visBVisualisation is needed to display the items and events in the ListViews
	 */
	private void initialiseListViews(VisBVisualisation visBVisualisation){
		clear();
		if (visBVisualisation != null) {
			this.visBEvents.setItems(FXCollections.observableArrayList(visBVisualisation.getVisBEvents()));
			this.visBItems.setItems(FXCollections.observableArrayList(visBVisualisation.getVisBItems()));
			for (final VisBEvent event : visBVisualisation.getVisBEvents()) {
				eventsById.put(event.getId(), event);
			}
		}
	}

	public void clear(){
		this.visBEvents.setItems(null);
		this.visBItems.setItems(null);
		this.eventsById.clear();
	}

	private void refresh() {
		visBItems.refresh();
		visBEvents.refresh();
	}


}


