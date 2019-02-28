package de.prob2.ui.error;

import java.io.CharArrayWriter;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import com.google.inject.Injector;

import de.prob.animator.domainobjects.ErrorItem;
import de.prob.exception.CliError;
import de.prob.exception.ProBError;
import de.prob2.ui.beditor.BEditorView;
import de.prob2.ui.internal.StageManager;

import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

public final class ExceptionAlert extends Alert {
	@FXML private VBox contentVBox;
	@FXML private Label label;
	@FXML private TreeTableView<Object> proBErrorTable;
	@FXML private TreeTableColumn<Object, Object> typeColumn;
	@FXML private TreeTableColumn<Object, Object> messageColumn;
	@FXML private TreeTableColumn<Object, Object> locationsColumn;
	@FXML private TextArea stackTraceTextArea;
	
	private final Injector injector;
	private final StageManager stageManager;
	private final ResourceBundle bundle;
	private final String text;
	private final Throwable exc;
	
	public ExceptionAlert(final Injector injector, final String text, final Throwable exc) {
		super(Alert.AlertType.NONE); // Alert type is set in FXML
		
		Objects.requireNonNull(exc);
		
		this.injector = injector;
		this.stageManager = injector.getInstance(StageManager.class);
		this.bundle = injector.getInstance(ResourceBundle.class);
		this.text = text;
		this.exc = exc;
		
		stageManager.loadFXML(this, "exception_alert.fxml");
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
		stageManager.register(this);
		ProBError proBError = null;
		CliError cliError = null;
		for (Throwable e = this.exc; e != null; e = e.getCause()) {
			if (e instanceof ProBError) {
				proBError = (ProBError)e;
				break;
			} else if (e instanceof CliError) {
				cliError = (CliError)e;
				break;
			}
		}
		final String message;
		if (cliError != null) {
			message = bundle.getString("error.exceptionAlert.cliErrorExplanation");
		} else if (proBError != null) {
			message = proBError.getOriginalMessage();
		} else {
			message = this.exc.getMessage();
		}
		this.label.setText(this.text + ":\n");
		
		if(message != null) {
			this.label.setText(this.label.getText() + message);
		}
		
		final Callback<TreeTableColumn.CellDataFeatures<Object, Object>, ObservableValue<Object>> cellValueFactory = features -> Bindings.createObjectBinding(() -> features.getValue().getValue());
		this.typeColumn.setCellValueFactory(cellValueFactory);
		this.messageColumn.setCellValueFactory(cellValueFactory);
		this.locationsColumn.setCellValueFactory(cellValueFactory);
		
		this.typeColumn.setCellFactory(col -> injector.getInstance(TypeCell.class));
		this.messageColumn.setCellFactory(col -> injector.getInstance(MessageCell.class));
		this.locationsColumn.setCellFactory(col -> injector.getInstance(LocationsCell.class));
		
		try (final CharArrayWriter caw = new CharArrayWriter(); final PrintWriter pw = new PrintWriter(caw)) {
			exc.printStackTrace(pw);
			this.stackTraceTextArea.setText(caw.toString());
		}
		
		if (proBError != null && proBError.getErrors() != null) {
			final TreeItem<Object> root = new TreeItem<>();
			final Map<String, List<ErrorItem>> grouped = groupErrorItemsByFile(proBError.getErrors());
			grouped.keySet().stream().sorted().forEach(fileName -> {
				final TreeItem<Object> ti = new TreeItem<>(fileName);
				root.getChildren().add(ti);
				grouped.get(fileName).stream()
					.map(TreeItem<Object>::new)
					.collect(Collectors.toCollection(ti::getChildren));
				ti.setExpanded(true);
			});
			final BEditorView bEditorView = injector.getInstance(BEditorView.class);
			final Path editorPath = bEditorView.getPath();
			if (editorPath != null && grouped.containsKey(editorPath.toString())) {
				bEditorView.highlightErrorLocations(
					grouped.get(editorPath.toString()).stream()
						.flatMap(item -> item.getLocations().stream())
						.collect(Collectors.toList())
				);
			}
			this.proBErrorTable.setRoot(root);
		} else {
			this.contentVBox.getChildren().remove(this.proBErrorTable);
		}
	}
}
