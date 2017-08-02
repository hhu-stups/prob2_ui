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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HelpTreeItem extends TreeItem<String>{
    private boolean isLeaf;
    private boolean isFirstTimeChildren = true;
    private boolean isFirstTimeLeaf = true;
    private File file;

    public HelpTreeItem(final File file) throws IOException {
        super(file.getName());
        this.file = file;
        if (file.isFile()) {
            String text = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            text = text.replaceAll("\\s+", " ");
            Pattern p = Pattern.compile("<title>(.*?)</title>");
            Matcher m = p.matcher(text);
            while (m.find()) {
                this.setValue(m.group(1));
            }
        } else {
            this.setValue(this.getValue().replace(File.separator,""));
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
            isLeaf = this.file.isFile();
        }
        return isLeaf;
    }

    private ObservableList<TreeItem<String>> buildChildren(HelpTreeItem helpTreeItem) throws IOException {
        File file = helpTreeItem.file;
        if (file != null && file.isDirectory()) {
            ObservableList<TreeItem<String>> children = FXCollections.observableArrayList();
            for (File child : file.listFiles()) {
                children.add(createNode(child));
            }
            return children;
        }
        return FXCollections.emptyObservableList();
    }

    private TreeItem<String> createNode(final File file) throws IOException {
        HelpTreeItem hti = new HelpTreeItem(file);
        if (hti.isLeaf()) {
            HelpSystem.fileMap.put(file, hti);
        }
        return hti;
    }

    public File getFile() {
        return file;
    }
}
