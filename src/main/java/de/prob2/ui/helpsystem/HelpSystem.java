package de.prob2.ui.helpsystem;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.prob2.ui.ProB2;
import de.prob2.ui.internal.StageManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import java.io.File;
import java.net.URISyntaxException;

@Singleton
public class HelpSystem extends SplitPane{
    @FXML private TreeView treeView;
    @FXML private WebView webView;
    public WebEngine webEngine;

    @Inject
    public HelpSystem(final StageManager stageManager) throws URISyntaxException {
        stageManager.loadFXML(this, "helpsystem.fxml");
        treeView.setRoot(createNode(new File(ProB2.class.getResource("/").toURI())));
        treeView.getRoot().setExpanded(true);
        treeView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (((HelpTreeItem) newVal).isLeaf()){
                //TODO Öffnen von HTML Files dem Baum entsprechend
                //System.out.println("Selected Text : " + ((TreeItem<String>) newVal).getValue());
            }
        });
        webEngine = webView.getEngine();
        webEngine.load("https://www3.hhu.de/stups/prob/index.php/Main_Page");
    }

    private TreeItem<String> createNode(final File f) {
        return new HelpTreeItem(f);
    }
}
