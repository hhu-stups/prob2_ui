package de.prob2.ui.helpsystem;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
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
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import de.prob.Main;
import de.prob2.ui.ProB2;
import de.prob2.ui.internal.StageManager;

import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import org.slf4j.LoggerFactory;

@Singleton
public class HelpSystem extends StackPane {
	@FXML private Button external;
	@FXML private TreeView<String> treeView;
	@FXML private WebView webView;
	WebEngine webEngine;
	private URI helpURI;
	boolean isJar;
	boolean isHelpButton;
	final String helpSubdirectoryString;
	static HashMap<File,HelpTreeItem> fileMap = new HashMap<>();

	@Inject
	public HelpSystem(final StageManager stageManager, final Injector injector) throws URISyntaxException, IOException {
		stageManager.loadFXML(this, "helpsystem.fxml");
		helpURI = ProB2.class.getClassLoader().getResource("help/").toURI();
		isJar = helpURI.toString().startsWith("jar:");
		isHelpButton = false;
		helpSubdirectoryString = findHelpSubdirectory();
		File helpSubdirectory = getHelpDirectory();

		treeView.setRoot(createNode(helpSubdirectory));
		treeView.setShowRoot(false);
		treeView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
			if (newVal!=null && newVal.isLeaf()){
				File f = ((HelpTreeItem) newVal).getFile();
				if (!isHelpButton) {
					webEngine.load(f.toURI().toString());
				} else {
					isHelpButton = false;
				}
			}
		});

		webEngine = webView.getEngine();
		webEngine.setUserStyleSheetLocation(this.getClass().getResource("help.css").toString());
		webEngine.setJavaScriptEnabled(true);
		webEngine.getLoadWorker().stateProperty().addListener((obs, oldVal, newVal) -> {
			if (newVal == Worker.State.SUCCEEDED) {
				String url = webEngine.getLocation();
				if (url.contains("http://") || url.contains("https://")) {
					webView.getEngine().getHistory().go(-1);
					injector.getInstance(ProB2.class).getHostServices().showDocument(url);
				}
				findMatchingTreeViewEntryToSelect(url);
			}
		});
		if (!treeView.getRoot().getChildren().isEmpty()) {
			webEngine.load(((HelpTreeItem) treeView.getRoot().getChildren().get(0)).getFile().toURI().toString());
		}

		external.setOnAction(e -> injector.getInstance(ProB2.class).getHostServices().showDocument("https://www3.hhu.de/stups/prob/index.php/Main_Page"));
		external.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.EXTERNAL_LINK_SQUARE));
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
		Files.createDirectories(target.getParent());
		Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				Path newdir = target.resolve(source.relativize(dir).toString());
				Files.copy(dir, newdir);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				if (file.toString().contains(".htm")||file.toString().contains(".png"))
					Files.copy(file, target.resolve(source.relativize(file).toString()), StandardCopyOption.REPLACE_EXISTING);
				return FileVisitResult.CONTINUE;
			}
		});
	}

	private File getHelpDirectory() throws IOException, URISyntaxException {
		if (isJar) {
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

	private void findMatchingTreeViewEntryToSelect(String url) {
		for (Map.Entry<File,HelpTreeItem> entry : fileMap.entrySet()) {
			final HelpTreeItem hti = entry.getValue();
			try {
				if (entry.getKey().toURI().toURL().sameFile(new URL(URLDecoder.decode(url,"UTF-8"))) ||
						URLDecoder.decode(url,"UTF-8").contains(entry.getKey().toString())) {
					expandTree(hti);
					Platform.runLater(() -> treeView.getSelectionModel().select(treeView.getRow(hti)));
				}
			} catch (MalformedURLException | UnsupportedEncodingException e) {
				LoggerFactory.getLogger(HelpSystem.class).error("URL not found", e);
			}
		}
	}

	private static String findHelpSubdirectory() {
		final String helpDirName = "help_" + Locale.getDefault().getLanguage();
		return HelpSystem.class.getResource("/help/" + helpDirName + ".txt") == null ? "help_en" : helpDirName;
	}
}
