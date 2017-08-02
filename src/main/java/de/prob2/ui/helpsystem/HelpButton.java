package de.prob2.ui.helpsystem;

import com.google.inject.Inject;
import com.google.inject.Injector;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import de.prob.Main;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.layout.FontSize;
import javafx.fxml.FXML;
import javafx.scene.control.Button;

import java.io.File;

public class HelpButton extends Button{
    private Injector injector;
    private File helpContent;

    @Inject
    private HelpButton(StageManager stageManager, Injector injector) {
        this.injector = injector;
        stageManager.loadFXML(this, "helpbutton.fxml");
    }

    @FXML
    public void initialize() {
        FontSize fontsize = injector.getInstance(FontSize.class);
        ((FontAwesomeIconView) (this.getGraphic())).glyphSizeProperty().bind(fontsize.multiply(2.0));
    }

    @FXML
    public void openHelp() {
        final HelpSystemStage helpSystemStage = injector.getInstance(HelpSystemStage.class);
        if (helpContent!=null) {
            helpSystemStage.setContent(helpContent);
        }
        helpSystemStage.show();
        helpSystemStage.toFront();
    }

    public void setHelpContent(String fileName) {
        helpContent = new File(Main.getProBDirectory() + "prob2ui" + File.separator + "help" + File.separator + fileName);
    }
}
