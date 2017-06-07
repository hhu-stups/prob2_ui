package de.prob2.ui.helpsystem;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.prob2.ui.internal.StageManager;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.ResourceBundle;

@Singleton
public class HelpSystemStage extends Stage {
    @Inject
    private HelpSystemStage(final StageManager stageManager, ResourceBundle bundle){
        this.setTitle(bundle.getString("helpsystem.stage.title"));
        this.setScene(new Scene(new HelpSystem(stageManager)));
        stageManager.register(this, this.getClass().getName());
    }

    public void setContent(String pathToHelp) {
        ((HelpSystem) this.getScene().getRoot()).webEngine.load(pathToHelp);
    }
}
