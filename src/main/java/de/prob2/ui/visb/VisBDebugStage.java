package de.prob2.ui.visb;


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

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import static de.prob2.ui.internal.JavascriptFunctionInvoker.buildInvocation;
import static de.prob2.ui.internal.JavascriptFunctionInvoker.wrapAsString;


@Singleton
public class VisBDebugStage extends Stage {

	private final StageManager stageManager;

	private final CurrentTrace currentTrace;

	private final CurrentProject currentProject;

	private final ResourceBundle bundle;

	private final Injector injector;

	@FXML
	private ListView<VisBItem> visBItems;
	@FXML
	private ListView<VisBEvent> visBEvents;

	private final Map<String, VisBEvent> itemsToEvent;

	@Inject
	public VisBDebugStage(final StageManager stageManager, final CurrentTrace currentTrace, final CurrentProject currentProject, final ResourceBundle bundle, final Injector injector) {
		super();
		this.stageManager = stageManager;
		this.currentTrace = currentTrace;
		this.currentProject = currentProject;
		this.bundle = bundle;
		this.injector = injector;
		this.itemsToEvent = new HashMap<>();
		this.stageManager.loadFXML(this, "visb_debug_stage.fxml");
	}

	@FXML
	public void initialize() {
		ChangeListener<VisBItem> listener = (observable, from, to) -> {
			if(from != null) {
				removeHighlighting(from);
			}
			if(to != null) {
				applyHighlighting(to);
			}
		};
		this.visBItems.setCellFactory(lv -> new ListViewItem(stageManager, currentTrace, bundle, injector, itemsToEvent));
		this.visBEvents.setCellFactory(lv -> new ListViewEvent(stageManager, bundle, injector));
		this.currentTrace.addListener((observable, from, to) -> refresh());
		this.currentProject.currentMachineProperty().addListener((observable, from, to) -> refresh());
		this.visBItems.getSelectionModel().selectedItemProperty().addListener(listener);
		this.setOnCloseRequest(e -> this.visBItems.getSelectionModel().clearSelection());
	}

	private void removeHighlighting(VisBItem item) {
		String id = item.getId();
		if(itemsToEvent.containsKey(id)) {
			for (VisBHover hover : itemsToEvent.get(id).getHovers()) {
				String invocation = buildInvocation("changeAttribute", wrapAsString(hover.getHoverID()), wrapAsString(hover.getHoverAttr()), wrapAsString(hover.getHoverLeaveVal()));
				injector.getInstance(VisBStage.class).runScript(invocation);
			}
		}
	}

	private void applyHighlighting(VisBItem item) {
		String id = item.getId();
		if(itemsToEvent.containsKey(id)) {
			for (VisBHover hover : itemsToEvent.get(id).getHovers()) {
				String invocation = buildInvocation("changeAttribute", wrapAsString(hover.getHoverID()), wrapAsString(hover.getHoverAttr()), wrapAsString(hover.getHoverEnterVal()));
				injector.getInstance(VisBStage.class).runScript(invocation);
			}
		}
	}

	/**
	 * After loading the JSON/ VisB file and preparing it in the {@link VisBController} the ListViews are initialised.
	 * @param visBVisualisation is needed to display the items and events in the ListViews
	 */
	public void initialiseListViews(VisBVisualisation visBVisualisation){
		clear();
		this.visBEvents.setItems(FXCollections.observableArrayList(visBVisualisation.getVisBEvents()));
		this.visBItems.setItems(FXCollections.observableArrayList(visBVisualisation.getVisBItems()));
		fillItemsToEvent(visBVisualisation.getVisBItems(), visBVisualisation.getVisBEvents());
	}

	public void updateItems(List<VisBItem> items) {
		this.visBItems.setItems(FXCollections.observableArrayList(items));
		if(itemsToEvent.isEmpty()) {
			fillItemsToEvent(items, visBEvents.getItems());
		}
	}

	private void fillItemsToEvent(List<VisBItem> visBItems, List<VisBEvent> visBEvents) {
		for(VisBItem item : visBItems) {
			for(VisBEvent event : visBEvents) {
				if(item.getId().equals(event.getId())) {
					itemsToEvent.put(item.getId(), event);
				}
			}
		}
	}

	public void clear(){
		this.visBEvents.setItems(null);
		this.visBItems.setItems(null);
		this.itemsToEvent.clear();
	}

	private void refresh() {
		visBItems.refresh();
		visBEvents.refresh();
	}


}


