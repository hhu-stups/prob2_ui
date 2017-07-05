package de.prob2.ui.helpsystem;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.prob2.ui.ProB2;
import de.prob2.ui.internal.StageManager;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;

@Singleton
public class HelpSystem extends StackPane {
    @FXML private TreeView<String> treeView;
    @FXML private WebView webView;
    WebEngine webEngine;
    static HashMap<File,HelpTreeItem> fileMap = new HashMap<>();

    @Inject
    public HelpSystem(final StageManager stageManager) throws URISyntaxException, IOException {
        stageManager.loadFXML(this, "helpsystem.fxml");
        File f = new File(ProB2.class.getClassLoader().getResource("help/").toURI());
        treeView.setRoot(createNode(f));
        treeView.setShowRoot(false);
        treeView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal!=null && newVal.isLeaf()){
                webEngine.load(((HelpTreeItem) newVal).getFile().toURI().toString());
            }
        });
        webEngine = webView.getEngine();
        webEngine.load(((HelpTreeItem) treeView.getRoot().getChildren().get(0)).getFile().toURI().toString());
        webEngine.getLoadWorker().stateProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal== Worker.State.SUCCEEDED) {
                HelpTreeItem hti = null;
                for (File file : fileMap.keySet()) {
                    try {
                        if (file.toURI().toURL().sameFile(new URL(webEngine.getLocation()))) {
                            hti = fileMap.get(file);
                        }
                    } catch (IOException e) {
                        LoggerFactory.getLogger(HelpSystem.class).error("Can not locate file",e);
                    }
                }
                if (hti!=null) {
                    treeView.getSelectionModel().select(treeView.getRow(hti));
                }
            }
        });
    }

    private TreeItem<String> createNode(final File f) throws IOException {
        HelpTreeItem hti = new HelpTreeItem(f);
        if (hti.isLeaf()) {
            fileMap.put(f, hti);
        }
        return hti;
    }
}
