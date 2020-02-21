package de.prob2.ui.visb.help;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import de.prob2.ui.error.ExceptionAlert;
import de.prob2.ui.internal.StageManager;
import javafx.fxml.FXML;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

@Singleton
public class UserManualStage extends Stage {
    @FXML
    private WebView userManualWebView;

    private Injector injector;

    @Inject
    public UserManualStage(final Injector injector, final StageManager stageManager) {
        super();
        stageManager.loadFXML(this, "user_manual_stage.fxml");
    }

    @FXML
    public void initialize(){
        try {
            this.userManualWebView.getEngine().load(this.getClass().getResource("user_manual.html").toExternalForm());
        } catch(Exception e){
            new ExceptionAlert(injector, "User manual cannot be shown.", e).show();
        }
    }
}
