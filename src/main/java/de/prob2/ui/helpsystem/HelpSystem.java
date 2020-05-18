package de.prob2.ui.helpsystem;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

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
	private final class HelpCell extends TreeCell<String> {
		private HelpCell() {
			super();
		}

		@Override
		protected void updateItem(final String item, final boolean empty) {
			super.updateItem(item, empty);

			if (empty) {
				this.setText(null);
			} else {
				this.setText(titleForPage(item));
			}
		}
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(HelpSystem.class);

	@FXML private Button external;
	@FXML private TreeView<String> treeView;
	@FXML private WebView webView;
	WebEngine webEngine;
	boolean isHelpButton;
	private final ResourceBundle helpPageTitles;
	private final ResourceBundle helpPageResourcePaths;
	private final Map<String, TreeItem<String>> itemsByKey;
	private final Map<URI, String> keysByUri;
	private final TreeItem<String> root;
	private Properties classToHelpFileMap;

	@Inject
	private HelpSystem(final StageManager stageManager, final Injector injector) {
		stageManager.loadFXML(this, "helpsystem.fxml");
		isHelpButton = false;

		this.helpPageTitles = ResourceBundle.getBundle("de.prob2.ui.helpsystem.help_page_titles");
		this.helpPageResourcePaths = ResourceBundle.getBundle("de.prob2.ui.helpsystem.help_page_resource_paths");
		this.itemsByKey = new HashMap<>(); // populated by findInTreeOrAdd
		this.keysByUri = this.helpPageResourcePaths.keySet().stream()
			.collect(Collectors.toMap(this::uriForPage, k -> k));

		this.root = new TreeItem<>();
		this.helpPageTitles.keySet().forEach(this::findInTreeOrAdd);
		sortTree(root, Comparator.comparing(item -> titleForPage(item.getValue())));
		root.setExpanded(true);
		treeView.setRoot(root);
		treeView.setShowRoot(false);
		treeView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
			if (newVal!=null && newVal.isLeaf()){
				final String key = newVal.getValue();
				if (!isHelpButton) {
					webEngine.load(this.uriForPage(key).toString());
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

		external.setOnAction(e -> injector.getInstance(ProB2.class).getHostServices().showDocument("https://www3.hhu.de/stups/prob/index.php/Main_Page"));

		this.openHelpForIdentifier(ProB2.class.getName());
	}

	private String titleForPage(final String key) {
		return this.helpPageTitles.getString(key);
	}

	private String resourcePathForPage(final String key) {
		return this.helpPageResourcePaths.getString(key);
	}
	
	/**
	 * Ensure that any Unicode characters in the given URI are percent-encoded. Java allows both encoded and unencoded Unicode characters in URIs, which can lead to two URIs being considered unequal even though they are equal when decoded.
	 * 
	 * @param uri the URI in which to percent-encode Unicode characters
	 * @return the input URI with all previously unencoded Unicode characters percent-encoded
	 * @throws URISyntaxException if the {@link URI#URI(String)} constructor fails to parse the new URI string
	 */
	private static URI ensureUnicodePercentEncoded(final URI uri) throws URISyntaxException {
		return new URI(uri.toASCIIString());
	}

	private URI uriForPage(final String key) {
		final String resourcePath = this.resourcePathForPage(key);
		final URL url = this.getClass().getResource(resourcePath);
		if (url == null) {
			throw new IllegalArgumentException("URL not found for resource path " + resourcePath + " for help page key " + key);
		}
		try {
			return ensureUnicodePercentEncoded(url.toURI());
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException("Could not convert URL " + url + " for resource path " + resourcePath + " for help page key " + key + " to URI", e);
		}
	}

	private Properties getClassToHelpFileMap() {
		if (this.classToHelpFileMap == null) {
			final String resourceName = "help/help.properties";
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

	/**
	 * <p>Find an item in the help tree that corresponds to the dot-separated path {@code key}. If no matching item exists, it is created and added to the tree (along with all necessary parent items).</p>
	 * <p>The value of every item in the tree is its full dot-separated path from the root. For example, the item {@code one.two.three} is located under {@code one.two}, which is located under {@code one}, which is located under the root.</p>
	 * 
	 * @param key the dot-separated path for which to find a matching item
	 * @return the item for the key
	 */
	private TreeItem<String> findInTreeOrAdd(final String key) {
		// Try to find a matching existing item in the map.
		if (this.itemsByKey.containsKey(key)) {
			return this.itemsByKey.get(key);
		}
		
		// Find the parent item based on the part of the key before the last dot (if any).
		final TreeItem<String> parent;
		final int indexOfLastDot = key.lastIndexOf('.');
		if (indexOfLastDot == -1) {
			// Key doesn't contain any dots, meaning it is top-level. Use the root item as the parent.
			parent = this.root;
		} else {
			// Key contains a dot. Split off the parent key (everything before the last dot) and use the corresponding item as the parent.
			// This continues recursively until a parent key is reached that has a matching item (or the root is reached, see above).
			parent = findInTreeOrAdd(key.substring(0, indexOfLastDot));
		}

		// Create a new item for the key and add it to the parent.
		final TreeItem<String> item = new TreeItem<>(key);
		parent.getChildren().add(item);
		this.itemsByKey.put(key, item);
		return item;
	}

	private static <T> void sortTree(final TreeItem<T> root, final Comparator<TreeItem<T>> comparator) {
		root.getChildren().sort(comparator);
		root.getChildren().forEach(item -> sortTree(item, comparator));
	}

	private void expandTree(TreeItem<?> ti) {
		if (ti!=null) {
			expandTree(ti.getParent());
			if (!ti.isLeaf()) {
				Platform.runLater(() -> ti.setExpanded(true));
			}
		}
	}

	private static URI removeFragment(final URI uri) throws URISyntaxException {
		return new URI(uri.getScheme(), uri.getSchemeSpecificPart(), null);
	}

	private void findMatchingTreeViewEntryToSelect(String url) {
		final URI uriWithoutFragment;
		try {
			uriWithoutFragment = ensureUnicodePercentEncoded(removeFragment(new URI(url)));
		} catch (URISyntaxException e) {
			LOGGER.warn("Help system web view navigated to an invalid URI: {}", url, e);
			return;
		}
		if (!keysByUri.containsKey(uriWithoutFragment)) {
			LOGGER.warn("No matching help tree item found for URI {}", uriWithoutFragment);
			return;
		}
		final TreeItem<String> hti = itemsByKey.get(keysByUri.get(uriWithoutFragment));
		expandTree(hti);
		Platform.runLater(() -> treeView.getSelectionModel().select(treeView.getRow(hti)));
	}

	public void openHelpForIdentifier(final String identifier) {
		String link = this.getHelpFileForIdentifier(identifier);
		final String helpPageKey;
		final String anchor;
		if (link.contains("#")) {
			int splitIndex = link.indexOf('#');
			helpPageKey = link.substring(0, splitIndex);
			anchor = link.substring(splitIndex);
		} else {
			helpPageKey = link;
			anchor = "";
		}
		final String url = this.uriForPage(helpPageKey) + anchor;
		LOGGER.debug("Opening URL in help: {}", url);
		this.webEngine.load(url);
	}
}
