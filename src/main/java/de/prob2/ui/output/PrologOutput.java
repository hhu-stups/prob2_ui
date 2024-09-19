package de.prob2.ui.output;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob.animator.IConsoleOutputListener;
import de.prob2.ui.internal.FXMLInjected;

import javafx.application.Platform;
import javafx.fxml.FXML;

import org.fxmisc.richtext.InlineCssTextArea;

@FXMLInjected
@Singleton
public final class PrologOutput extends InlineCssTextArea {
	private final IConsoleOutputListener outputListener;

	@Inject
	public PrologOutput() {
		this.outputListener = line -> Platform.runLater(() -> {
			// Remove trailing ANSI escape for resetting formatting, if any.
			// handleOutput already resets the formatting for each new line.
			final String lineToFormat = line.replaceAll("\\u001b\\[0m$", "") + "\n";
			String[] messageAndStyle = handleOutput(lineToFormat);
			this.appendText(messageAndStyle[0]);
			this.setStyle(Math.max(this.getLength()-messageAndStyle[0].length(), 0), this.getLength(), messageAndStyle[1]);
		});
	}

	@FXML
	private void initialize() {
		this.setPrefWidth(Double.MAX_VALUE);
		this.setWrapText(true);
		this.setEditable(false);
	}

	public IConsoleOutputListener getOutputListener() {
		return this.outputListener;
	}

	private static String[] handleOutput(String s) {
		// default settings
		String fontColor = "black";
		boolean underline = false;
		boolean strikethrough = false;
		String visibility = "visible";
		String weight = "NORMAL";
		String posture = "NORMAL";
		String backgroundColor = "white";

		String message = s;
		while(!message.isEmpty() && message.charAt(0) == 27) {
			// ANSI escape code found. Warning: If ANSI escape code is not ending with m some parts of messages might be lost!
			// Warning: If codes are not set at beginning of message, e.g. for changing attributes midmessage, chaos might ensue ;)
			int indexOfANSIEscapeCodeEnd = message.indexOf('m');
			if (indexOfANSIEscapeCodeEnd != -1) {
				String str = message.substring(1, indexOfANSIEscapeCodeEnd);
				// Setting supported font color and attributes for output
				switch (str) {
					case "[0":
						// Reset code. Return to default values.
						fontColor = "black";
						underline = false;
						strikethrough = false;
						visibility = "visible";
						weight = "NORMAL";
						posture = "NORMAL";
						backgroundColor = "white";
						break;
					case "[1":
						weight = "BOLD";
						break;
					case "[3":
						posture = "ITALIC";
						break;
					case "[4":
						underline = true;
						break;
					case "[8":
						visibility = "hidden";
						break;
					case "[9":
						strikethrough = true;
						break;
					case "[22":
						fontColor = "black";
						weight = "NORMAL";
						break;
					case "[23":
						posture = "NORMAL";
						break;
					case "[24":
						underline = false;
						break;
					case "[28":
						visibility = "visible";
						break;
					case "[29":
						strikethrough = false;
						break;
					case "[30":
						fontColor = "black";
						break;
					case "[31":
						fontColor = "red";
						break;
					case "[32":
						fontColor = "green";
						break;
					case "[33":
						fontColor = "yellow";
						break;
					case "[34":
						fontColor = "blue";
						break;
					case "[35":
						fontColor = "magenta";
						break;
					case "[36":
						fontColor = "cyan";
						break;
					case "[37":
						fontColor = "white";
						break;
					case "[40":
						backgroundColor = "black";
						break;
					case "[41":
						backgroundColor = "red";
						break;
					case "[42":
						backgroundColor = "green";
						break;
					case "[43":
						backgroundColor = "yellow";
						break;
					case "[44":
						backgroundColor = "blue";
						break;
					case "[45":
						backgroundColor = "magenta";
						break;
					case "[46":
						backgroundColor = "cyan";
						break;
					case "[47":
						backgroundColor = "white";
						break;
					default:
						// Code not supported (yet?). Do nothing.
						break;
				}
			}
			message = message.substring(indexOfANSIEscapeCodeEnd + 1);
		}

		String style = 	"-fx-fill: " + fontColor + "; " +
						"-fx-underline: " + underline + "; " +
						"visibility: " + visibility + "; " +
						"-fx-strikethrough: " + strikethrough + "; " +
						"-fx-font-weight: " + weight + "; " +
						"-fx-font-style: " + posture + "; " +
						"-rtfx-background-color: " + backgroundColor;

		return new String[] {message, style};
	}
}
