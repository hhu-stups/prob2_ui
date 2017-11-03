package de.prob2.ui.helpsystem;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob.Main;

import de.prob2.ui.ProB2;
import de.prob2.ui.internal.StageManager;

import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import org.slf4j.LoggerFactory;

@Singleton
public class HelpSystem extends StackPane {
	@FXML private TreeView<String> treeView;
	@FXML private WebView webView;
	WebEngine webEngine;
	static HashMap<File,HelpTreeItem> fileMap = new HashMap<>();

	@Inject
	public HelpSystem(final StageManager stageManager) throws URISyntaxException, IOException {
		stageManager.loadFXML(this, "helpsystem.fxml");
		URI uri = ProB2.class.getClassLoader().getResource("help/").toURI();
		File helpMainDirectory = getHelpMainDirectory(uri);

		treeView.setRoot(createNode(helpMainDirectory));
		treeView.setShowRoot(false);
		treeView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
			if (newVal!=null && newVal.isLeaf()){
				File f = ((HelpTreeItem) newVal).getFile();
				webEngine.load(f.toURI().toString());
			}
		});

		webEngine = webView.getEngine();
		webEngine.setJavaScriptEnabled(true);
		webEngine.getLoadWorker().stateProperty().addListener((obs, oldVal, newVal) -> {
			if (newVal == Worker.State.SUCCEEDED) {
				findMatchingTreeViewEntryToSelect();
			}
		});
		webEngine.load(((HelpTreeItem) treeView.getRoot().getChildren().get(0)).getFile().toURI().toString());
	}

	private TreeItem<String> createNode(final File file) throws IOException {
		HelpTreeItem hti = new HelpTreeItem(file);
		if (!file.getName().contains(":")) {
			Platform.runLater(() -> hti.setExpanded(true));
			if (hti.isLeaf()) {
				fileMap.put(file, hti);
			}
		}
		return hti;
	}

	private void expandTree(TreeItem<?> ti) {
		if (ti!=null) {
			expandTree(ti.getParent());
			if (!ti.isLeaf()) {
				Platform.runLater(() -> ti.setExpanded(true));
			}
		}
	}

	private void copyHelp(Path source, Path target) throws IOException {
		Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				Path newdir = target.resolve(source.relativize(dir).toString());
				Files.copy(dir, newdir);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Files.copy(file, target.resolve(source.relativize(file).toString()), StandardCopyOption.REPLACE_EXISTING);
				return FileVisitResult.CONTINUE;
			}
		});
	}

	private File getHelpMainDirectory(URI uri) throws IOException {
		if (uri.toString().startsWith("jar:")) {
			Path target = Paths.get(Main.getProBDirectory() + "prob2ui" + File.separator + "help");
			Map<String, String> env = new HashMap<>();
			env.put("create", "true");
			try (FileSystem jarFileSystem = FileSystems.newFileSystem(uri, env)) {
				Path source = jarFileSystem.getPath("/help/");
				if (!target.toFile().exists()) {
					copyHelp(source, target);
				}
			}
			return new File(Main.getProBDirectory() + "prob2ui" + File.separator +"help");
		} else {
			return new File(uri);
		}
	}

	private void findMatchingTreeViewEntryToSelect() {
		for (Map.Entry<File,HelpTreeItem> entry : fileMap.entrySet()) {
			final HelpTreeItem hti = entry.getValue();
			try {
				if (entry.getKey().toURI().toURL().sameFile(new URL(webEngine.getLocation()))) {
					expandTree(hti);
					Platform.runLater(() -> treeView.getSelectionModel().select(treeView.getRow(hti)));
				}
			} catch (MalformedURLException e) {
				LoggerFactory.getLogger(HelpSystem.class).error("Malformed URL", e);
			}
		}
	}
}
