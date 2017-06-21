package de.prob2.ui.helpsystem;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;
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
            while (m.find() == true) {
                this.setValue(m.group(1));
            }
        }
    }

    @Override public ObservableList<TreeItem<String>> getChildren() {
        if (isFirstTimeChildren) {
            isFirstTimeChildren = false;
            try {
                super.getChildren().setAll(buildChildren(this));
            } catch (IOException e) {
                e.printStackTrace();
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

    private TreeItem<String> createNode(final File f) throws IOException {
        return new HelpTreeItem(f);
    }

    public File getFile() {
        return f;
    }
}
