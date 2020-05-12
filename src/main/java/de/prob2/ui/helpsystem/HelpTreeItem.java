package de.prob2.ui.helpsystem;

import java.io.File;

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
			ObservableList<TreeItem<String>> children = FXCollections.observableArrayList();
			for (File child : this.file.listFiles()) {
				if (child.isDirectory() || child.getName().contains(".html")) {
					children.add(createNode(child));
				}
			}
			this.getChildren().setAll(children);
		}
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
