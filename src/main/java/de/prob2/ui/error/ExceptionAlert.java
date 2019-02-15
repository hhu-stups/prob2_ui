package de.prob2.ui.error;

import java.io.CharArrayWriter;
import java.io.PrintWriter;
import java.util.Objects;
import java.util.ResourceBundle;

import com.google.inject.Injector;

import de.prob.animator.domainobjects.ErrorItem;
import de.prob.exception.CliError;
import de.prob.exception.ProBError;
import de.prob2.ui.internal.StageManager;

import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
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
		
		final Callback<TableColumn.CellDataFeatures<ErrorItem, ErrorItem>, ObservableValue<ErrorItem>> cellValueFactory = features -> Bindings.createObjectBinding(features::getValue);
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
			this.proBErrorTable.getItems().setAll(proBError.getErrors());
		} else {
			this.contentVBox.getChildren().remove(this.proBErrorTable);
		}
	}
}
