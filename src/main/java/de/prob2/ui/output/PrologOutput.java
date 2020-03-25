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
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
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
			Paint fontColor = Color.BLACK;
			Text output = new Text();
			boolean underline = false;
			boolean visible = true;
			FontWeight weight = FontWeight.NORMAL;
			boolean hasANSICode = false;
			while (s.charAt(0) == 27) {
				hasANSICode = true;
				// removing escape
				s = s.substring(1);
				// setting font color and attributes for output and removing remaining ANSI code from string
				if (s.startsWith("[31m")) {
					s = s.replace("[31m", "");
					fontColor = Color.RED;
				} else if (s.startsWith("[32m")) {
					s = s.replace("[32m", "");
					fontColor = Color.GREEN;
				} else if (s.startsWith("[33m")) {
					s = s.replace("[33m", "");
					fontColor = Color.YELLOW;
				} else if (s.startsWith("[34m")) {
					s = s.replace("[34m", "");
					fontColor = Color.BLUE;
				} else if (s.startsWith("[35m")) {
					s = s.replace("[35m", "");
					fontColor = Color.MAGENTA;
				} else if (s.startsWith("[36m")) {
					s = s.replace("[36m", "");
					fontColor = Color.CYAN;
				} else if (s.startsWith("[37m")) {
					s = s.replace("[37m", "");
					fontColor = Color.WHITE;
				} else if (s.startsWith("[1m")) {
					s = s.replace("[1m", "");
					weight = FontWeight.BOLD;
				} else if (s.startsWith("[4m")) {
					s = s.replace("[4m", "");
					underline = true;
				} else if (s.startsWith("[5m")) {
					s = s.replace("[5m", "");
					// not supported
				} else if (s.startsWith("[7m")) {
					s = s.replace("[7m", "");
					// not supported
				} else if (s.startsWith("[8m")) {
					s = s.replace("[8m", "");
					visible = false;
				}
			}

			// remove ANSI code identifier
			if (hasANSICode && s.startsWith("! ")) {
				s = s.replace("! ", "");
			}

			output.setFont(Font.font(output.getFont().getFamily(), weight, output.getFont().getSize()));
			output.setText(s);
			output.setFill(fontColor);
			output.setUnderline(underline);
			output.setVisible(visible);
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
