package de.prob2.ui.error;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import com.google.inject.Inject;

import de.prob.animator.domainobjects.ErrorItem;
import de.prob.exception.CliError;
import de.prob.exception.ProBError;
import de.prob2.ui.internal.ErrorDisplayFilter;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;

public final class ExceptionAlert extends Alert {
	@FXML private VBox contentVBox;
	@FXML private Label label;
	@FXML private ErrorTableView proBErrorTable;
	@FXML private TextArea stackTraceTextArea;
	
	private final StageManager stageManager;
	private final ErrorDisplayFilter errorDisplayFilter;
	private final I18n i18n;
	private final StringProperty text;
	private final ObjectProperty<Throwable> exception;
	private final StringProperty exceptionMessage;
	
	@Inject
	private ExceptionAlert(final StageManager stageManager, final ErrorDisplayFilter errorDisplayFilter, final I18n i18n) {
		super(Alert.AlertType.NONE, null, ButtonType.OK); // Alert type is set in FXML, DefaultButtons will be overwritten to make x-Button functional

		this.stageManager = stageManager;
		this.errorDisplayFilter = errorDisplayFilter;
		this.i18n = i18n;
		
		this.text = new SimpleStringProperty(this, "text", null);
		this.exception = new SimpleObjectProperty<>(this, "exception", null);
		this.exceptionMessage = new SimpleStringProperty(this, "exceptionMessage", "");

		stageManager.loadFXML(this, "exception_alert.fxml");
	}
	
	@FXML
	private void initialize() {
		stageManager.register(this);
		this.label.textProperty().bind(Bindings.concat(this.textProperty(), ":\n", this.exceptionMessage));
		this.exceptionProperty().addListener(this::createAndShowExceptionMessage);
		this.setButtons();
	}

	private void createAndShowExceptionMessage(ObservableValue<? extends Throwable> o, Throwable from, Throwable to) {
		ProBError proBError = null;
		CliError cliError = null;
		for (Throwable e = to; e != null; e = e.getCause()) {
			if (e instanceof ProBError) {
				proBError = (ProBError) e;
				break;
			} else if (e instanceof CliError) {
				cliError = (CliError) e;
				break;
			}
		}
		final String message;
		if (cliError != null) {
			message = this.i18n.translate("error.exceptionAlert.cliErrorExplanation", cliError.getMessage());
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
		if (proBError != null) {
			this.contentVBox.getChildren().add(this.proBErrorTable);
			final List<ErrorItem> filteredErrors = this.errorDisplayFilter.filterErrors(proBError.getErrors());
			this.proBErrorTable.getErrorItems().setAll(filteredErrors);
		}
	}

	private void setButtons() {
		ButtonType copyToClipBoardButtonType = new ButtonType(this.i18n.translate("common.buttons.copyToClipboard"));
		this.getDialogPane().getButtonTypes().add(copyToClipBoardButtonType);
		this.getDialogPane().lookupButton(copyToClipBoardButtonType).addEventFilter(ActionEvent.ACTION, ae -> {
			this.stackTraceTextArea.selectAll();
			this.stackTraceTextArea.copy();
			this.stackTraceTextArea.deselect();
			ae.consume();
		});

		ButtonType okButton = new ButtonType("OK", ButtonType.CANCEL.getButtonData());
		this.getDialogPane().getButtonTypes().add(okButton);
	}

	public static String getExceptionStackTrace(final Throwable throwable) {
		try (final StringWriter sw = new StringWriter(); final PrintWriter pw = new PrintWriter(sw)) {
			throwable.printStackTrace(pw);
			return sw.toString();
		} catch (IOException ignored) {
			return ""; // can never happen
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
