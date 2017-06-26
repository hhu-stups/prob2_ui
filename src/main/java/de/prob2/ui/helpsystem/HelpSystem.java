package de.prob2.ui.helpsystem;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.ProB2;
import de.prob2.ui.internal.StageManager;

import javafx.fxml.FXML;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

@Singleton
public class HelpSystem extends StackPane {
    @FXML private TreeView<String> treeView;
    @FXML private WebView webView;
    WebEngine webEngine;

    @Inject
    public HelpSystem(final StageManager stageManager) throws URISyntaxException, IOException {
        stageManager.loadFXML(this, "helpsystem.fxml");
        File f = new File(ProB2.class.getClassLoader().getResource("help/").toURI());
        treeView.setRoot(createNode(f));
        treeView.setShowRoot(false);
        treeView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.isLeaf()){
                webEngine.load(((HelpTreeItem) newVal).getFile().toURI().toString());
            }
        });
        webEngine = webView.getEngine();
        webEngine.load(((HelpTreeItem) treeView.getRoot().getChildren().get(0)).getFile().toURI().toString());
    }

    private TreeItem<String> createNode(final File f) throws IOException {
        return new HelpTreeItem(f);
    }
}
