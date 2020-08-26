package de.prob2.ui.output;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.OutputStreamAppender;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.prob.cli.ProBInstance;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;

@Singleton
public class PrologOutput extends TextFlow {

	private static class PrologOutputAppender extends OutputStream {
		StringBuilder sb;
		TextFlow textFlow;
		private PrologOutputAppender(TextFlow textFlow) {
			this.sb = new StringBuilder();
			this.textFlow = textFlow;
		}

		@Override
		public void write(int b) {
			Platform.runLater(() -> {
				//Append each character to a StringBuilder until the string terminator and only set the final result in the end.
				//This avoids the UI from hanging up.
				sb.append((char) b);
				if(b == 10) {
					Text output = handleOutput(sb.toString());
					this.textFlow.getChildren().add(output);
					this.sb = new StringBuilder();
				}
			});
		}

		private Text handleOutput(String s) {
			Text output = new Text();
			// default settings
			Paint fontColor = Color.BLACK;
			boolean underline = false;
			boolean blinking = false;
			double blinkingTime = 1;
			boolean visible = true;
			boolean strikethrough = false;
			FontWeight weight = FontWeight.NORMAL;
			FontPosture posture = FontPosture.REGULAR;
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
							fontColor = Color.BLACK;
							underline = false;
							blinking = false;
							blinkingTime = 1;
							visible = true;
							strikethrough = false;
							weight = FontWeight.NORMAL;
							posture = FontPosture.REGULAR;
							break;
						case "[1":
							weight = FontWeight.BOLD;
							break;
						case "[3":
							posture = FontPosture.ITALIC;
							break;
						case "[4":
							underline = true;
							break;
						case "[5":
							blinking = true;
							blinkingTime = 1;
							break;
						case "[6":
							blinking = true;
							blinkingTime = 0.5;
							break;
						case "[8":
							visible = false;
							break;
						case "[9":
							strikethrough = true;
							break;
						case "[22":
							fontColor = Color.BLACK;
							weight = FontWeight.NORMAL;
							break;
						case "[23":
							posture = FontPosture.REGULAR;
							break;
						case "[24":
							underline = false;
							break;
						case "[25":
							blinking = false;
							break;
						case "[28":
							visible = true;
							break;
						case "[29":
							strikethrough = false;
							break;
						case "[31":
							fontColor = Color.RED;
							break;
						case "[32":
							fontColor = Color.GREEN;
							break;
						case "[33":
							fontColor = Color.YELLOW;
							break;
						case "[34":
							fontColor = Color.BLUE;
							break;
						case "[35":
							fontColor = Color.MAGENTA;
							break;
						case "[36":
							fontColor = Color.CYAN;
							break;
						case "[37":
							fontColor = Color.WHITE;
							break;
						default:
							// Code not supported (yet?). Do nothing.
							break;
					}
				}
				message = message.substring(indexOfANSIEscapeCodeEnd + 1);
			}

			output.setFont(Font.font(output.getFont().getFamily(), weight, posture, output.getFont().getSize()));
			output.setText(message);
			output.setFill(fontColor);
			output.setUnderline(underline);
			if (blinking) {
				FadeTransition fadeTransition = new FadeTransition(Duration.seconds(blinkingTime), output);
				fadeTransition.setToValue(0);
				fadeTransition.setFromValue(1);
				fadeTransition.setCycleCount(Animation.INDEFINITE);
				fadeTransition.play();
			}
			output.setVisible(visible);
			output.setStrikethrough(strikethrough);

			return output;
		}
	}

	@Inject
	public PrologOutput() {
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
