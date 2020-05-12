package de.prob2.ui.helpsystem;

import java.io.File;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;

class HelpTreeItem extends TreeItem<String>{
	private File file;

	HelpTreeItem(final File file) {
		super(file.getName());
		this.file = file;
		if (file.isFile()) {
			this.setValue(file.getName().replace(".html",""));
		} else {
			this.setValue(this.getValue().replace(File.separator,""));
		}
		this.getChildren().setAll(buildChildren(this));
		this.setExpanded(true);
		Platform.runLater(() -> this.setExpanded(false));
	}

	private ObservableList<TreeItem<String>> buildChildren(HelpTreeItem helpTreeItem) {
		File f = helpTreeItem.file;
		if (f != null && f.isDirectory()) {
			ObservableList<TreeItem<String>> children = FXCollections.observableArrayList();
			for (File child : f.listFiles()) {
				if ((child.isDirectory() && !child.getName().contains("screenshots"))||child.getName().contains(".html")) {
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
