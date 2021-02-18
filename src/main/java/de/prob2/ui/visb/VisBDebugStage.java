package de.prob2.ui.visb;


import com.google.inject.Injector;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.visb.ui.ListViewEvent;
import de.prob2.ui.visb.ui.ListViewItem;
import de.prob2.ui.visb.visbobjects.VisBEvent;
import de.prob2.ui.visb.visbobjects.VisBItem;
import de.prob2.ui.visb.visbobjects.VisBVisualisation;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.stage.Stage;

import javax.inject.Inject;
import javax.inject.Singleton;
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

    @Inject
    public VisBDebugStage(final StageManager stageManager, final CurrentTrace currentTrace, final CurrentProject currentProject, final ResourceBundle bundle, final Injector injector) {
        super();
        this.stageManager = stageManager;
        this.currentTrace = currentTrace;
        this.currentProject = currentProject;
        this.bundle = bundle;
        this.injector = injector;
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
		this.visBItems.setCellFactory(lv -> new ListViewItem(stageManager, currentTrace, bundle, injector));
		this.visBEvents.setCellFactory(lv -> new ListViewEvent(stageManager, bundle, injector));
        this.currentTrace.addListener((observable, from, to) -> refresh());
		this.currentProject.currentMachineProperty().addListener((observable, from, to) -> refresh());
		this.visBItems.getSelectionModel().selectedItemProperty().addListener(listener);
		this.setOnCloseRequest(e -> this.visBItems.getSelectionModel().clearSelection());
    }

    private void removeHighlighting(VisBItem item) {
		String id = item.getId();
		String invocation = buildInvocation("changeAttribute", wrapAsString(id), wrapAsString("opacity"), wrapAsString("1.0"));
		// TO DO: if initial opacity was not 1.0, this does *not* reset the opacity to its initial value !!!
		injector.getInstance(VisBStage.class).runScript(invocation);
	}

	private void applyHighlighting(VisBItem item) {
		String id = item.getId();
		String invocation = buildInvocation("changeAttribute", wrapAsString(id), wrapAsString("opacity"), wrapAsString("0.5"));
		// TO DO: maybe we can find better ways of highlighting an object, maybe using filters ?
		injector.getInstance(VisBStage.class).runScript(invocation);
	}

    /**
     * After loading the JSON/ VisB file and preparing it in the {@link VisBController} the ListViews are initialised.
     * @param visBVisualisation is needed to display the items and events in the ListViews
     */
    public void initialiseListViews(VisBVisualisation visBVisualisation){
    	clear();
		this.visBItems.setItems(FXCollections.observableArrayList(visBVisualisation.getVisBItems()));
		this.visBEvents.setItems(FXCollections.observableArrayList(visBVisualisation.getVisBEvents()));
    }

    public void clear(){
        this.visBEvents.setItems(null);
        this.visBItems.setItems(null);
    }

    private void refresh() {
		visBItems.refresh();
		visBEvents.refresh();
	}


}


