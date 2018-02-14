package de.prob2.ui.helpsystem;

import java.awt.Desktop;
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
import java.util.Locale;
import java.util.Map;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob.Main;

import de.prob2.ui.ProB2;
import de.prob2.ui.internal.StageManager;

import de.prob2.ui.persistence.UIState;
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
	URI helpURI;
	UIState uiState;
	String helpSubdirectoryString = "help_en";
	static HashMap<File,HelpTreeItem> fileMap = new HashMap<>();
	// this needs to be updated if new translations of help are added
	private String[] languages = {"de", "en"};

	@Inject
	public HelpSystem(final StageManager stageManager, final UIState uiState) throws URISyntaxException, IOException {
		stageManager.loadFXML(this, "helpsystem.fxml");
		helpURI = ProB2.class.getClassLoader().getResource("help/").toURI();
		for (String language : languages) {
			if (isCurrentLanguage(language)) {
				helpSubdirectoryString = "help_" + language;
			}
		}
		File helpSubdirectory = getHelpDirectory();


		treeView.setRoot(createNode(helpSubdirectory));
		treeView.setShowRoot(false);
		treeView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
			if (newVal!=null && newVal.isLeaf()){
				File f = ((HelpTreeItem) newVal).getFile();
				webEngine.load(f.toURI().toString());
			}
		});

		webEngine = webView.getEngine();
		webEngine.setUserStyleSheetLocation(this.getClass().getResource("help.css").toString());
		webEngine.setJavaScriptEnabled(true);
		webEngine.getLoadWorker().stateProperty().addListener((obs, oldVal, newVal) -> {
			String url = webEngine.getLoadWorker().getMessage().trim().replace("Loading ","");
			if (url.contains("http://") || url.contains("https://")) {
				webEngine.getLoadWorker().cancel();
				try {
					// xdg-open opens two(!) tabs, prevention?
					// Runtime.getRuntime().exec() seems to trigger fatal error in java <- updating java has not fixed this, resorting to Desktop class
					/*if (Runtime.getRuntime().exec(new String[] { "which", "xdg-open" }).getInputStream().read() != -1) {
						Runtime.getRuntime().exec(new String[] { "xdg-open", url });
					} else {*/
					// FIXME (Unsupported Desktop class seems to be a KDE problem...)
					// FIXME (Doesn't seem to work on my Gnome Classic or Gnome desktops either although java.awt.Desktop should at least be supported here...)
						Desktop.getDesktop().browse(new URI(url));
					//}
				} catch (IOException | URISyntaxException e) {
					LoggerFactory.getLogger(HelpSystem.class).error("Can not load URL in external browser", e);
				}
			} else if (newVal == Worker.State.SUCCEEDED) {
				findMatchingTreeViewEntryToSelect();
			}
		});
		webEngine.load(((HelpTreeItem) treeView.getRoot().getChildren().get(0)).getFile().toURI().toString());
	}

	private TreeItem<String> createNode(final File file) {
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

	private File getHelpDirectory() throws IOException, URISyntaxException {
		if (helpURI.toString().startsWith("jar:")) {
			Path target = Paths.get(Main.getProBDirectory() + "prob2ui" + File.separator + "help");
			Map<String, String> env = new HashMap<>();
			env.put("create", "true");
			try (FileSystem jarFileSystem = FileSystems.newFileSystem(helpURI, env)) {
				Path source = jarFileSystem.getPath("/help/");
				if (!target.toFile().exists()) {
					copyHelp(source, target);
				}
			}
			return new File(Main.getProBDirectory() + "prob2ui" + File.separator + "help" + File.separator + helpSubdirectoryString);
		} else {
			return new File(ProB2.class.getClassLoader().getResource("help/" + helpSubdirectoryString).toURI());
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

	private boolean isCurrentLanguage(String language) {
		try {
			return uiState.getLocaleOverride().toString().startsWith(language);
		} catch (NullPointerException e) {
			return Locale.getDefault().toString().startsWith(language);
		}
	}
}