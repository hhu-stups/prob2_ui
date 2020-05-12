package de.prob2.ui.helpsystem;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob.Main;
import de.prob2.ui.ProB2;
import de.prob2.ui.internal.StageManager;

import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class HelpSystem extends StackPane {
	private static final class HelpCell extends TreeCell<File> {
		private HelpCell() {
			super();
		}

		@Override
		protected void updateItem(final File item, final boolean empty) {
			super.updateItem(item, empty);

			if (empty) {
				this.setText(null);
			} else if (item.isFile()) {
				this.setText(item.getName().replace(".html", ""));
			} else {
				this.setText(item.getName().replace(File.separator, ""));
			}
		}
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(HelpSystem.class);

	@FXML private Button external;
	@FXML private TreeView<File> treeView;
	@FXML private WebView webView;
	WebEngine webEngine;
	private URI helpURI;
	boolean isJar;
	boolean isHelpButton;
	final String helpSubdirectoryString;
	private final Map<File, TreeItem<File>> fileMap = new HashMap<>();
	private Properties classToHelpFileMap;

	@Inject
	private HelpSystem(final StageManager stageManager, final Injector injector) throws URISyntaxException, IOException {
		stageManager.loadFXML(this, "helpsystem.fxml");
		helpURI = ProB2.class.getClassLoader().getResource("help/").toURI();
		isJar = helpURI.toString().startsWith("jar:");
		isHelpButton = false;
		helpSubdirectoryString = findHelpSubdirectory();
		extractHelpFiles();

		final TreeItem<File> root = createNode(this.getHelpSubdirectory());
		root.setExpanded(true);
		treeView.setRoot(root);
		treeView.setShowRoot(false);
		treeView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
			if (newVal!=null && newVal.isLeaf()){
				File f = newVal.getValue();
				if (!isHelpButton) {
					webEngine.load(f.toURI().toString());
				} else {
					isHelpButton = false;
				}
			}
		});
		treeView.setCellFactory(tv -> new HelpCell());

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
			webEngine.load(treeView.getRoot().getChildren().get(0).getValue().toURI().toString());
		}

		external.setOnAction(e -> injector.getInstance(ProB2.class).getHostServices().showDocument("https://www3.hhu.de/stups/prob/index.php/Main_Page"));

		final File defaultPage = new File(this.getHelpSubdirectory(), "ProB2UI.html");
		this.openHelpPage(defaultPage, "");
	}

	private Properties getClassToHelpFileMap() {
		if (this.classToHelpFileMap == null) {
			final String resourceName = "help/" + this.helpSubdirectoryString + ".properties";
			try (InputStream stream = this.getClass().getClassLoader().getResourceAsStream(resourceName)) {
				if (stream != null) {
					try (final Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
						this.classToHelpFileMap = new Properties();
						this.classToHelpFileMap.load(reader);
					}
				} else {
					LOGGER.error("Help file mapping does not exist: {}", resourceName);
				}
			} catch (IOException e) {
				LOGGER.error("IOException while reading help file mapping", e);
			}
		}
		return this.classToHelpFileMap;
	}

	String getHelpFileForIdentifier(final String identifier) {
		return this.getClassToHelpFileMap().getProperty(identifier);
	}

	File getHelpSubdirectory() {
		if (this.isJar) {
			return new File(Main.getProBDirectory() +
					"prob2ui" + File.separator +
					"help" + File.separator +
					this.helpSubdirectoryString);
		} else {
			try {
				return new File(ProB2.class.getClassLoader().getResource(
						"help/" +
						this.helpSubdirectoryString).toURI());
			} catch (URISyntaxException e) {
				throw new AssertionError("Help directory URL is not a valid URI", e);
			}
		}
	}

	private TreeItem<File> createNode(final File file) {
		final TreeItem<File> item = new TreeItem<>(file);
		if (file.isDirectory()) {
			Arrays.stream(file.listFiles())
				.filter(child -> child.isDirectory() || child.getName().contains(".html"))
				.map(this::createNode)
				.collect(Collectors.toCollection(item::getChildren));
		} else {
			fileMap.put(file, item);
		}
		return item;
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

	private void extractHelpFiles() throws IOException {
		if (isJar) {
			Path target = Paths.get(Main.getProBDirectory() + "prob2ui" + File.separator + "help");
			try (FileSystem jarFileSystem = FileSystems.newFileSystem(helpURI, Collections.emptyMap())) {
				Path source = jarFileSystem.getPath("/help/");
				if (!target.toFile().exists()) {
					copyHelp(source, target);
				}
			}
		}
	}

	private void findMatchingTreeViewEntryToSelect(String url) {
		for (Map.Entry<File, TreeItem<File>> entry : fileMap.entrySet()) {
			final TreeItem<File> hti = entry.getValue();
			try {
				if (entry.getKey().toURI().toURL().sameFile(new URL(URLDecoder.decode(url,"UTF-8"))) ||
						URLDecoder.decode(url,"UTF-8").contains(entry.getKey().toString())) {
					expandTree(hti);
					Platform.runLater(() -> treeView.getSelectionModel().select(treeView.getRow(hti)));
				}
			} catch (MalformedURLException | UnsupportedEncodingException e) {
				LOGGER.error("URL not found", e);
			}
		}
	}

	private static String findHelpSubdirectory() {
		final String helpDirName = "help_" + Locale.getDefault().getLanguage();
		return HelpSystem.class.getResource("/help/" + helpDirName + ".properties") == null ? "help_en" : helpDirName;
	}

	public void openHelpPage(File file, String anchor) {
		final String url = file.toURI() + anchor;
		LOGGER.debug("Opening URL in help: {}", url);
		this.webEngine.load(url);
	}

	public void openHelpForIdentifier(final String identifier) {
		File main = this.getHelpSubdirectory();
		String link = this.getHelpFileForIdentifier(identifier);
		final String htmlFile;
		final String anchor;
		if (link.contains("#")) {
			int splitIndex = link.indexOf('#');
			htmlFile = link.substring(0, splitIndex);
			anchor = link.substring(splitIndex);
		} else {
			htmlFile = link;
			anchor = "";
		}
		final URI htmlFileUri;
		try {
			// Use the multi-arg URI constructor to quote (percent-encode) the htmlFile path.
			// This is needed for help files with spaces in the path, which are not valid URIs without quoting the spaces first.
			htmlFileUri = new URI(null, htmlFile, null);
		} catch (URISyntaxException exc) {
			throw new AssertionError("Invalid help file name", exc);
		}
		final File file = new File(main.toURI().resolve(htmlFileUri));
		this.openHelpPage(file, anchor);
	}
}
