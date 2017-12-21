package de.prob2.ui.internal;

import java.io.CharArrayWriter;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.ResourceBundle;

import com.google.inject.Injector;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;

import de.prob.animator.domainobjects.ErrorItem;
import de.prob.exception.ProBError;

import de.prob2.ui.menu.EditPreferencesProvider;

import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

public final class ExceptionAlert extends Alert {
	@FXML private VBox contentVBox;
	@FXML private Label label;
	@FXML private TableView<ErrorItem> proBErrorTable;
	@FXML private TableColumn<ErrorItem, ErrorItem> typeColumn;
	@FXML private TableColumn<ErrorItem, ErrorItem> messageColumn;
	@FXML private TableColumn<ErrorItem, ErrorItem> locationsColumn;
	@FXML private TextArea stackTraceTextArea;
	
	private final StageManager stageManager;
	private final ResourceBundle bundle;
	private final EditPreferencesProvider editMenu;
	private final String text;
	private final Throwable exc;
	
	public ExceptionAlert(final Injector injector, final String text, final Throwable exc) {
		super(Alert.AlertType.NONE); // Alert type is set in FXML
		
		Objects.requireNonNull(exc);
		
		this.stageManager = injector.getInstance(StageManager.class);
		this.bundle = injector.getInstance(ResourceBundle.class);
		this.editMenu = injector.getInstance(EditPreferencesProvider.class);
		this.text = text;
		this.exc = exc;
		
		stageManager.loadFXML(this, "exception_alert.fxml");
	}
	
	@FXML
	private void initialize() {
		stageManager.register(this);
		ProBError proBError = null;
		for (Throwable e = this.exc; e != null; e = e.getCause()) {
			if (e instanceof ProBError) {
				proBError = (ProBError)e;
				break;
			}
		}
		final String message;
		if (proBError != null) {
			message = proBError.getOriginalMessage();
		} else {
			message = this.exc.getMessage();
		}
		this.label.setText(this.text + ":\n" + message);
		
		final Callback<TableColumn.CellDataFeatures<ErrorItem, ErrorItem>, ObservableValue<ErrorItem>> cellValueFactory = features -> Bindings.createObjectBinding(features::getValue);
		this.typeColumn.setCellValueFactory(cellValueFactory);
		this.messageColumn.setCellValueFactory(cellValueFactory);
		this.locationsColumn.setCellValueFactory(cellValueFactory);
		
		this.typeColumn.setCellFactory(col -> new TableCell<ErrorItem, ErrorItem>() {
			@Override
			protected void updateItem(final ErrorItem item, final boolean empty) {
				super.updateItem(item, empty);
				
				if (empty || item == null) {
					this.setText(null);
				} else {
					final String typeName;
					switch (item.getType()) {
						case WARNING:
							typeName = bundle.getString("exceptionAlert.proBErrorTable.type.warning");
							break;
						
						case ERROR:
							typeName = bundle.getString("exceptionAlert.proBErrorTable.type.error");
							break;
						
						case INTERNAL_ERROR:
							typeName = bundle.getString("exceptionAlert.proBErrorTable.type.internalError");
							break;
						
						default:
							typeName = item.getType().name();
					}
					this.setText(typeName);
				}
			}
		});
		
		this.messageColumn.setCellFactory(col -> new TableCell<ErrorItem, ErrorItem>() {
			@Override
			protected void updateItem(final ErrorItem item, final boolean empty) {
				super.updateItem(item, empty);
				
				if (empty || item == null) {
					this.setText(null);
				} else {
					this.setText(item.getMessage());
				}
			}
		});
		
		this.locationsColumn.setCellFactory(col -> {
			final TableCell<ErrorItem, ErrorItem> cell = new TableCell<ErrorItem, ErrorItem>() {
				@Override
				protected void updateItem(final ErrorItem item, final boolean empty) {
					super.updateItem(item, empty);
					
					if (empty || item == null) {
						this.setGraphic(null);
					} else {
						final VBox vbox = new VBox();
						for (final ErrorItem.Location location : item.getLocations()) {
							final Button openLocationButton = new Button(null, new FontAwesomeIconView(FontAwesomeIcon.PENCIL));
							openLocationButton.setOnAction(event -> openLocationInEditor(location));
							final Label label = new Label(location.toString());
							label.setTextOverrun(OverrunStyle.LEADING_ELLIPSIS);
							final HBox hbox = new HBox(openLocationButton, label);
							HBox.setHgrow(openLocationButton, Priority.NEVER);
							HBox.setHgrow(label, Priority.ALWAYS);
							hbox.setAlignment(Pos.CENTER_LEFT);
							vbox.getChildren().add(hbox);
						}
						this.setGraphic(vbox);
					}
				}
			};
			return cell;
		});
		
		try (final CharArrayWriter caw = new CharArrayWriter(); final PrintWriter pw = new PrintWriter(caw)) {
			exc.printStackTrace(pw);
			this.stackTraceTextArea.setText(caw.toString());
		}
		
		if (proBError != null && proBError.getErrors() != null) {
			this.proBErrorTable.getItems().setAll(proBError.getErrors());
		} else {
			this.contentVBox.getChildren().remove(this.proBErrorTable);
		}
	}
	
	private void openLocationInEditor(final ErrorItem.Location location) {
		// TODO Jump to error location in file
		editMenu.showEditorStage(Paths.get(location.getFilename()));
	}
}
