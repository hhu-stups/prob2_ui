package de.prob2.ui.visb;


import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.visb.ui.ListViewEvent;
import de.prob2.ui.visb.ui.ListViewItem;
import de.prob2.ui.visb.visbobjects.VisBEvent;
import de.prob2.ui.visb.visbobjects.VisBItem;
import de.prob2.ui.visb.visbobjects.VisBVisualisation;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.stage.Stage;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ResourceBundle;


@Singleton
public class VisBDebugStage extends Stage {

    private final StageManager stageManager;

    private final CurrentTrace currentTrace;

    private final ResourceBundle bundle;

    @FXML
    private ListView<VisBItem> visBItems;
    @FXML
    private ListView<VisBEvent> visBEvents;

    @Inject
    public VisBDebugStage(final StageManager stageManager, final CurrentTrace currentTrace, final ResourceBundle bundle) {
        super();
        this.stageManager = stageManager;
        this.currentTrace = currentTrace;
        this.bundle = bundle;
        this.stageManager.loadFXML(this, "visb_debug_stage.fxml");
    }

    @FXML
    public void initialize() {
		this.visBItems.setCellFactory(lv -> new ListViewItem(stageManager, currentTrace, bundle));
		this.visBEvents.setCellFactory(lv -> new ListViewEvent(stageManager));
        this.currentTrace.addListener((observable, from, to) -> {
            visBItems.refresh();
            visBEvents.refresh();
        });
    }

    /**
     * After loading the JSON/ VisB file and preparing it in the {@link VisBController} the ListViews are initialised.
     * @param visBVisualisation is needed to display the items and events in the ListViews
     */
    void initialiseListViews(VisBVisualisation visBVisualisation){
        this.visBItems.setItems(FXCollections.observableArrayList(visBVisualisation.getVisBItems()));
        this.visBEvents.setItems(FXCollections.observableArrayList(visBVisualisation.getVisBEvents()));
    }

    public void clear(){
        this.visBEvents.setItems(null);
        this.visBItems.setItems(null);
    }


}


