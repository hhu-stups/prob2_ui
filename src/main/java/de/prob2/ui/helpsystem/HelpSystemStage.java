package de.prob2.ui.helpsystem;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.prob2.ui.internal.StageManager;
import javafx.stage.Stage;

@Singleton
public class HelpSystemStage extends Stage {
    @Inject
    private HelpSystemStage(final StageManager stageManager){
        stageManager.loadFXML(this, "helpsystemstage.fxml", this.getClass().getName());
    }
}
