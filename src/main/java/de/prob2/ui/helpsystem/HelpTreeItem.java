package de.prob2.ui.helpsystem;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;

import java.io.File;

public class HelpTreeItem extends TreeItem<String>{
    private boolean isLeaf;
    private boolean isFirstTimeChildren = true;
    private boolean isFirstTimeLeaf = true;
    private File f;

    public HelpTreeItem(final File f) {
        super(f.getName());
        this.f = f;
    }

    @Override public ObservableList<TreeItem<String>> getChildren() {
        if (isFirstTimeChildren) {
            isFirstTimeChildren = false;
            super.getChildren().setAll(buildChildren(this));
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

    private ObservableList<TreeItem<String>> buildChildren(HelpTreeItem helpTreeItem) {
        File f = helpTreeItem.f;
        if (f != null && f.isDirectory()) {
            File[] files = f.listFiles();
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

    private TreeItem<String> createNode(final File f) {
        return new HelpTreeItem(f);
    }

    public File getFile() {
        return f;
    }
}
