package de.prob2.ui.helpsystem;

import java.io.File;
import java.io.IOException;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;

import org.slf4j.LoggerFactory;

public class HelpTreeItem extends TreeItem<String>{
	private boolean isLeaf;
	private boolean isFirstTimeChildren = true;
	private boolean isFirstTimeLeaf = true;
	private File file;

	public HelpTreeItem(final File file) {
		super(file.getName());
		this.file = file;
		if (file.isFile()) {
			this.setValue(file.getName().replace(".md.html",""));
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
		File f = helpTreeItem.file;
		if (f != null && f.isDirectory()) {
			ObservableList<TreeItem<String>> children = FXCollections.observableArrayList();
			for (File child : f.listFiles()) {
				if ((child.isDirectory() && !child.getName().contains("screenshots"))||child.getName().contains(".md.html")) {
					children.add(createNode(child));
				}
			}
			return children;
		}
		return FXCollections.emptyObservableList();
	}

	private TreeItem<String> createNode(final File file) {
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
