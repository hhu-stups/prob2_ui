package de.prob2.ui.output;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.OutputStreamAppender;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.prob.cli.ProBInstance;
import javafx.application.Platform;
import org.fxmisc.richtext.InlineCssTextArea;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;

@Singleton
public class PrologOutput extends InlineCssTextArea {

	private static class PrologOutputAppender extends OutputStream {
		StringBuilder sb;
		InlineCssTextArea textArea;
		private PrologOutputAppender(InlineCssTextArea styleClassedTextArea) {
			this.sb = new StringBuilder();
			this.textArea = styleClassedTextArea;
		}

		@Override
		public void write(int b) {
			Platform.runLater(() -> {
				//Append each character to a StringBuilder until the string terminator and only set the final result in the end.
				//This avoids the UI from hanging up.
				sb.append((char) b);
				if(b == 10) {
					String[] messageAndStyle = handleOutput(sb.toString());
					textArea.replaceText(textArea.getLength(), textArea.getLength(), messageAndStyle[0]);
					textArea.setStyle(textArea.getLength()-messageAndStyle[0].length(), textArea.getLength(), messageAndStyle[1]);
					this.sb = new StringBuilder();
				}
			});
		}

		private String[] handleOutput(String s) {
			// default settings
			String fontColor = "black";
			boolean underline = false;
			boolean strikethrough = false;
			String visibility = "visible";
			String weight = "NORMAL";
			String posture = "NORMAL";

			String message = s;
			while(!message.isEmpty() && message.charAt(0) == 27) {
				// ANSI escape code found. Warning: If ANSI escape code is not ending with m some parts of messages might be lost!
				// Warning: If codes are not set at beginning of message, e.g. for changing attributes midmessage, chaos might ensue ;)
				int indexOfANSIEscapeCodeEnd = s.indexOf("m");
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
						case "[25":
							//blinking = false;
							break;
						case "[28":
							visibility = "visible";
							break;
						case "[29":
							strikethrough = false;
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
						default:
							// Code not supported (yet?). Do nothing.
							break;
					}
				}
				message = message.substring(indexOfANSIEscapeCodeEnd + 1);
			}

			String style = 	"-fx-fill: " + fontColor + "; " +
							"-fx-underline: " + underline + "; " +
							"visiblility: " + visibility + "; " +
							"-fx-strikethrough: " + strikethrough + "; " +
							"-fx-font-weight: " + weight + "; " +
							"-fx-font-style: " + posture;

			return new String[] {message, style};
		}
	}

	@Inject
	public PrologOutput() {
		this.setPrefWidth(Double.MAX_VALUE);
		this.setWrapText(true);
		PrologOutputAppender prologOutputAppender = new PrologOutputAppender(this);
		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

		Thread thread = new Thread(() -> {
			PatternLayoutEncoder encoder = new PatternLayoutEncoder();
			encoder.setContext(context);
			encoder.setPattern("%replace(%msg){'\\u001b\\[0m', ''}%n");
			encoder.start();

			OutputStreamAppender<ILoggingEvent> outputStreamAppender = new OutputStreamAppender<>();
			outputStreamAppender.setContext(context);
			outputStreamAppender.setEncoder(encoder);
			outputStreamAppender.setOutputStream(prologOutputAppender);
			outputStreamAppender.start();

			Logger log = (Logger) LoggerFactory.getLogger(ProBInstance.class);
			log.addAppender(outputStreamAppender);
		});
		thread.start();
	}

}
