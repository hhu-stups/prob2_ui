package de.prob2.ui.helpsystem;

import java.io.File;
import java.util.Arrays;
import java.util.stream.Collectors;

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
			Arrays.stream(this.file.listFiles())
				.filter(child -> child.isDirectory() || child.getName().contains(".html"))
				.map(this::createNode)
				.collect(Collectors.toCollection(this::getChildren));
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
