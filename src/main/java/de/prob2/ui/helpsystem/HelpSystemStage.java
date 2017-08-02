package de.prob2.ui.helpsystem;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.prob.Main;
import de.prob2.ui.internal.StageManager;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ResourceBundle;

@Singleton
public class HelpSystemStage extends Stage {
    @Inject
    private HelpSystemStage(final StageManager stageManager, ResourceBundle bundle) throws URISyntaxException, IOException {
        this.setTitle(bundle.getString("helpsystem.stage.title"));
        this.setScene(new Scene(new HelpSystem(stageManager)));
        stageManager.register(this, this.getClass().getName());
        setContent(new File(Main.getProBDirectory() + "prob2ui" + File.separator + "help" + File.separator + "HelpMain.html"));
    }

    public void setContent(File file) {
        Platform.runLater(() -> ((HelpSystem) this.getScene().getRoot()).webEngine.load(file.toURI().toString()));
    }
}
