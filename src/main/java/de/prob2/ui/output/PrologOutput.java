package de.prob2.ui.output;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.core.OutputStreamAppender;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.prob.cli.ProBInstance;
import javafx.application.Platform;
import javafx.scene.control.TextArea;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;

@Singleton
public class PrologOutput extends TextArea {
	private class PrologOutputAppender extends OutputStream {
		TextArea textArea;
		private PrologOutputAppender(TextArea textArea) {
			this.textArea = textArea;
		}

		@Override
		public void write(int b) {
			Platform.runLater(() -> this.textArea.appendText(String.valueOf((char) b)));
		}
	}

	@Inject
	public PrologOutput() {
		this.setEditable(false);
		this.setWrapText(true);
		this.setScrollTop(Double.MAX_VALUE);
		PrologOutputAppender prologOutputAppender = new PrologOutputAppender(this);
		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

		PatternLayoutEncoder encoder = new PatternLayoutEncoder();
		encoder.setContext(context);
		encoder.setPattern("%replace(%msg){'\\[0m', ''}%n");
		encoder.start();

		OutputStreamAppender outputStreamAppender = new OutputStreamAppender();
		outputStreamAppender.setContext(context);
		outputStreamAppender.setEncoder(encoder);
		outputStreamAppender.setOutputStream(prologOutputAppender);
		outputStreamAppender.start();

		Logger log = (Logger) LoggerFactory.getLogger(ProBInstance.class);
		log.addAppender(outputStreamAppender);
	}
}
