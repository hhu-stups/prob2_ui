package de.prob2.ui.helpsystem;

import java.io.File;

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
	}

	public File getFile() {
		return file;
	}
}
