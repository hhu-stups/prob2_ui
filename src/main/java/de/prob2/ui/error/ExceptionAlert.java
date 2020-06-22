package de.prob2.ui.error;

import java.io.CharArrayWriter;
import java.io.PrintWriter;
import java.util.ResourceBundle;

import com.google.inject.Inject;

import de.prob.exception.CliError;
import de.prob.exception.ProBError;
import de.prob2.ui.internal.StageManager;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;

public final class ExceptionAlert extends Alert {
	@FXML private VBox contentVBox;
	@FXML private Label label;
	@FXML private ErrorTableView proBErrorTable;
	@FXML private TextArea stackTraceTextArea;
	
	private final StageManager stageManager;
	private final ResourceBundle bundle;
	private final StringProperty text;
	private final ObjectProperty<Throwable> exception;
	private final StringProperty exceptionMessage;
	
	@Inject
	private ExceptionAlert(final StageManager stageManager, final ResourceBundle bundle) {
		super(Alert.AlertType.NONE); // Alert type is set in FXML
		
		this.stageManager = stageManager;
		this.bundle = bundle;
		
		this.text = new SimpleStringProperty(this, "text", null);
		this.exception = new SimpleObjectProperty<>(this, "exception", null);
		this.exceptionMessage = new SimpleStringProperty(this, "exceptionMessage", "");
		
		stageManager.loadFXML(this, "exception_alert.fxml");
	}
	
	@FXML
	private void initialize() {
		stageManager.register(this);
		this.label.textProperty().bind(Bindings.concat(this.textProperty(), ":\n", this.exceptionMessage));
		this.exceptionProperty().addListener((o, from, to) -> {
			ProBError proBError = null;
			CliError cliError = null;
			for (Throwable e = to; e != null; e = e.getCause()) {
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
				message = String.format(bundle.getString("error.exceptionAlert.cliErrorExplanation"), cliError.getMessage());
			} else if (proBError != null) {
				message = proBError.getOriginalMessage();
			} else if (to != null) {
				message = to.getMessage();
			} else {
				message = null;
			}
			this.exceptionMessage.set(message == null ? "" : message);
			this.stackTraceTextArea.setText(to == null ? null : getExceptionStackTrace(to));
			
			this.contentVBox.getChildren().remove(this.proBErrorTable);
			if (proBError != null && proBError.getErrors() != null) {
				this.contentVBox.getChildren().add(this.proBErrorTable);
				this.proBErrorTable.getErrorItems().setAll(proBError.getErrors());
			}
		});
	}
	
	private static String getExceptionStackTrace(final Throwable throwable) {
		try (final CharArrayWriter caw = new CharArrayWriter(); final PrintWriter pw = new PrintWriter(caw)) {
			throwable.printStackTrace(pw);
			return caw.toString();
		}
	}
	
	public StringProperty textProperty() {
		return this.text;
	}
	
	public String getText() {
		return this.textProperty().get();
	}
	
	public void setText(final String text) {
		this.textProperty().set(text);
	}
	
	public ObjectProperty<Throwable> exceptionProperty() {
		return this.exception;
	}
	
	public Throwable getException() {
		return this.exceptionProperty().get();
	}
	
	public void setException(final Throwable exception) {
		this.exceptionProperty().set(exception);
	}
}
