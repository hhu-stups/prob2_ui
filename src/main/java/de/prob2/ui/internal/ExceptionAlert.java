package de.prob2.ui.internal;

import java.io.CharArrayWriter;
import java.io.PrintWriter;
import java.util.Objects;
import java.util.ResourceBundle;

import com.google.inject.Injector;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;

import de.prob.animator.domainobjects.ErrorItem;
import de.prob.exception.CliError;
import de.prob.exception.ProBError;
import de.prob2.ui.beditor.BEditorView;


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
	private class ErrorTypeCell extends TableCell<ErrorItem, ErrorItem> {
		@Override
		protected void updateItem(final ErrorItem item, final boolean empty) {
			super.updateItem(item, empty);
			
			if (empty || item == null) {
				this.setText(null);
			} else {
				final String typeName;
				switch (item.getType()) {
					case WARNING:
						typeName = bundle.getString("internal.exceptionAlert.proBErrorTable.type.warning");
						break;
					
					case ERROR:
						typeName = bundle.getString("internal.exceptionAlert.proBErrorTable.type.error");
						break;
					
					case INTERNAL_ERROR:
						typeName = bundle.getString("internal.exceptionAlert.proBErrorTable.type.internalError");
						break;
					
					default:
						typeName = item.getType().name();
				}
				this.setText(typeName);
			}
		}
	}
	
	private static class ErrorMessageCell extends TableCell<ErrorItem, ErrorItem> {
		@Override
		protected void updateItem(final ErrorItem item, final boolean empty) {
			super.updateItem(item, empty);
			
			if (empty || item == null) {
				this.setText(null);
			} else {
				this.setText(item.getMessage());
			}
		}
	}
	
	private class ErrorLocationsCell extends TableCell<ErrorItem, ErrorItem> {
		@Override
		protected void updateItem(final ErrorItem item, final boolean empty) {
			super.updateItem(item, empty);
			
			if (empty || item == null) {
				this.setGraphic(null);
			} else {
				final VBox vbox = new VBox();
				for (final ErrorItem.Location location : item.getLocations()) {
					final Button openLocationButton = new Button(null, new FontAwesomeIconView(FontAwesomeIcon.PENCIL));
					openLocationButton.setOnAction(event -> {
						final BEditorView bEditorView = injector.getInstance(BEditorView.class);
						bEditorView.selectRange(
							location.getStartLine()-1, location.getStartColumn(),
							location.getEndLine()-1, location.getEndColumn()
						);
					});
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
	}
	
	@FXML private VBox contentVBox;
	@FXML private Label label;
	@FXML private TableView<ErrorItem> proBErrorTable;
	@FXML private TableColumn<ErrorItem, ErrorItem> typeColumn;
	@FXML private TableColumn<ErrorItem, ErrorItem> messageColumn;
	@FXML private TableColumn<ErrorItem, ErrorItem> locationsColumn;
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
			message = bundle.getString("internal.exceptionAlert.cliErrorExplanation");
		} else if (proBError != null) {
			message = proBError.getOriginalMessage();
		} else {
			message = this.exc.getMessage();
		}
		this.label.setText(this.text + ":\n");
		
		if(message != null) {
			this.label.setText(this.label.getText() + message);
		}
		
		final Callback<TableColumn.CellDataFeatures<ErrorItem, ErrorItem>, ObservableValue<ErrorItem>> cellValueFactory = features -> Bindings.createObjectBinding(features::getValue);
		this.typeColumn.setCellValueFactory(cellValueFactory);
		this.messageColumn.setCellValueFactory(cellValueFactory);
		this.locationsColumn.setCellValueFactory(cellValueFactory);
		
		this.typeColumn.setCellFactory(col -> new ErrorTypeCell());
		this.messageColumn.setCellFactory(col -> new ErrorMessageCell());
		this.locationsColumn.setCellFactory(col -> new ErrorLocationsCell());
		
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
}
