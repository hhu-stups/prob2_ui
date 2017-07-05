package de.prob2.ui.helpsystem;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HelpTreeItem extends TreeItem<String>{
    private boolean isLeaf;
    private boolean isFirstTimeChildren = true;
    private boolean isFirstTimeLeaf = true;
    private File f;

    public HelpTreeItem(final File f) throws IOException {
        super(f.getName());
        this.f = f;
        if (!f.isDirectory()) {
            String text = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
            text = text.replaceAll("\\s+", " ");
            Pattern p = Pattern.compile("<title>(.*?)</title>");
            Matcher m = p.matcher(text);
            while (m.find()) {
                this.setValue(m.group(1));
            }
        }
        this.setExpanded(true);
        Platform.runLater(() -> this.setExpanded(false));
    }

    @Override public ObservableList<TreeItem<String>> getChildren() {
        if (isFirstTimeChildren) {
            isFirstTimeChildren = false;
            try {
                super.getChildren().setAll(buildChildren(this));
            } catch (IOException e) {
                LoggerFactory.getLogger(HelpTreeItem.class).error("Can not build children",e);
            }
        }
        return super.getChildren();
    }

    @Override public boolean isLeaf() {
        if (isFirstTimeLeaf) {
            isFirstTimeLeaf = false;
            isLeaf = this.f.isFile();
        }
        return isLeaf;
    }

    private ObservableList<TreeItem<String>> buildChildren(HelpTreeItem helpTreeItem) throws IOException {
        File file = helpTreeItem.f;
        if (file != null && file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                ObservableList<TreeItem<String>> children = FXCollections.observableArrayList();
                for (File childFile : files) {
                    children.add(createNode(childFile));
                }
                return children;
            }
        }

        return FXCollections.emptyObservableList();
    }

    private TreeItem<String> createNode(final File f) throws IOException {
        HelpTreeItem hti = new HelpTreeItem(f);
        if (hti.isLeaf()) {
            HelpSystem.fileMap.put(f, hti);
        }
        return hti;
    }

    public File getFile() {
        return f;
    }
}
