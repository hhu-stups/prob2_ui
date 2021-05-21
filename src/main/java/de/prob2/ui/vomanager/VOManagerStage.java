package de.prob2.ui.vomanager;


import de.prob2.ui.internal.StageManager;
import javafx.stage.Stage;

import javax.inject.Inject;

public class VOManagerStage extends Stage {

    @Inject
    public VOManagerStage(final StageManager stageManager) {
        super();
        stageManager.loadFXML(this, "vo_manager_view.fxml");
    }

}
