package de.prob2.ui.helpsystem;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.prob2.ui.internal.StageManager;
import javafx.fxml.FXML;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

@Singleton
public class HelpSystem extends SplitPane{
    @FXML private TreeView treeView;
    @FXML private WebView webView;
    public WebEngine webEngine;

    @Inject
    public HelpSystem(final StageManager stageManager) {
        stageManager.loadFXML(this, "helpsystem.fxml");
        TreeItem<String> root = new TreeItem<>("Root Node");
        root.setExpanded(true);
        root.getChildren().addAll(
                new TreeItem<>("Item 1"),
                new TreeItem<>("Item 2"),
                new TreeItem<>("Item 3")
        );
        treeView.setRoot(root);
        //treeView.setMaxWidth(0);
        webEngine = webView.getEngine();
        webEngine.load("https://www3.hhu.de/stups/prob/index.php/Main_Page");
    }
}
