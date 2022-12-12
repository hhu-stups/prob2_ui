package de.prob2.ui.error;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.inject.Inject;
import com.google.inject.Provider;

import de.prob.animator.domainobjects.ErrorItem;
import de.prob2.ui.beditor.BEditorView;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;

import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.util.Callback;

@FXMLInjected
public final class ErrorTableView extends TreeTableView<Object> {
	@FXML private TreeTableColumn<Object, Object> typeColumn;
	@FXML private TreeTableColumn<Object, Object> messageColumn;
	@FXML private TreeTableColumn<Object, Object> locationsColumn;
	
	private final Provider<TypeCell> typeCellProvider;
	private final Provider<MessageCell> messageCellProvider;
	private final Provider<LocationsCell> locationsCellProvider;
	private final Provider<BEditorView> bEditorViewProvider;
	
	private final ObservableList<ErrorItem> errorItems;

	private boolean syncWithEditor = true;
	
	@Inject
	private ErrorTableView(
		final StageManager stageManager,
		final Provider<TypeCell> typeCellProvider,
		final Provider<MessageCell> messageCellProvider,
		final Provider<LocationsCell> locationsCellProvider,
		final Provider<BEditorView> bEditorViewProvider
	) {
		super();
		
		this.typeCellProvider = typeCellProvider;
		this.messageCellProvider = messageCellProvider;
		this.locationsCellProvider = locationsCellProvider;
		this.bEditorViewProvider = bEditorViewProvider;
		
		this.errorItems = FXCollections.observableArrayList();
		
		stageManager.loadFXML(this, "error_table_view.fxml");
	}
	
	private static Map<String, List<ErrorItem>> groupErrorItemsByFile(final List<ErrorItem> errorItems) {
		final Map<String, List<ErrorItem>> grouped = new HashMap<>();
		for (final ErrorItem errorItem : errorItems) {
			if (errorItem.getLocations().isEmpty()) {
				grouped.computeIfAbsent("(location unknown)", k -> new ArrayList<>()).add(errorItem);
			} else {
				for (final ErrorItem.Location location : errorItem.getLocations()) {
					grouped.computeIfAbsent(location.getFilename(), k -> new ArrayList<>()).add(errorItem);
				}
			}
		}
		return grouped;
	}
	
	@FXML
	private void initialize() {
		final Callback<TreeTableColumn.CellDataFeatures<Object, Object>, ObservableValue<Object>> cellValueFactory = features -> Bindings.createObjectBinding(() -> features.getValue().getValue());
		this.typeColumn.setCellValueFactory(cellValueFactory);
		this.messageColumn.setCellValueFactory(cellValueFactory);
		this.locationsColumn.setCellValueFactory(cellValueFactory);
		
		this.typeColumn.setCellFactory(col -> this.typeCellProvider.get());
		this.messageColumn.setCellFactory(col -> this.messageCellProvider.get());
		this.locationsColumn.setCellFactory(col -> this.locationsCellProvider.get());
		
		this.getErrorItems().addListener((InvalidationListener)o -> {
			final TreeItem<Object> root = new TreeItem<>();
			final Map<String, List<ErrorItem>> grouped = groupErrorItemsByFile(this.getErrorItems());
			grouped.keySet().stream().sorted().forEach(fileName -> {
				final TreeItem<Object> ti = new TreeItem<>(fileName);
				root.getChildren().add(ti);
				grouped.get(fileName).stream()
					.map(TreeItem<Object>::new)
					.collect(Collectors.toCollection(ti::getChildren));
				ti.setExpanded(true);
			});
			if (syncWithEditor) {
				this.bEditorViewProvider.get().getErrors().addAll(this.getErrorItems());
			}
			this.setRoot(root);
		});
	}
	
	public ObservableList<ErrorItem> getErrorItems() {
		return errorItems;
	}

	public void dontSyncWithEditor() {
		this.syncWithEditor = false;
	}
}
