package de.prob2.ui.internal;

import java.io.CharArrayWriter;
import java.io.PrintWriter;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import de.prob.animator.domainobjects.ErrorItem;
import de.prob.exception.ProBError;

import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
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
	private final String text;
	private final Throwable exc;
	
	public ExceptionAlert(final StageManager stageManager, final ResourceBundle bundle, final String text, final Throwable exc) {
		super(Alert.AlertType.NONE); // Alert type is set in FXML
		
		Objects.requireNonNull(exc);
		
		this.stageManager = stageManager;
		this.bundle = bundle;
		this.text = text;
		this.exc = exc;
		
		stageManager.loadFXML(this, "exception_alert.fxml");
	}
	
	@FXML
	private void initialize() {
		stageManager.register(this);
		final String message;
		if (this.exc instanceof ProBError) {
			message = ((ProBError)this.exc).getOriginalMessage();
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
						this.setText(null);
					} else {
						this.setText(item.getLocations().stream().map(ErrorItem.Location::toString).collect(Collectors.joining("\n")));
					}
				}
			};
			cell.setTextOverrun(OverrunStyle.LEADING_ELLIPSIS);
			return cell;
		});
		
		try (final CharArrayWriter caw = new CharArrayWriter(); final PrintWriter pw = new PrintWriter(caw)) {
			exc.printStackTrace(pw);
			this.stackTraceTextArea.setText(caw.toString());
		}
		
		if (exc instanceof ProBError) {
			this.proBErrorTable.getItems().setAll(((ProBError)exc).getErrors());
		} else {
			this.contentVBox.getChildren().remove(this.proBErrorTable);
		}
	}
}
