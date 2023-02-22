package de.prob2.ui;


import com.google.inject.Guice;
import com.google.inject.Injector;
import de.prob2.ui.config.RuntimeOptions;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.ProB2Module;
import de.prob2.ui.project.ProjectManager;
import javafx.stage.Stage;
import org.testfx.framework.junit5.ApplicationTest;

public class TestBase extends ApplicationTest {
	protected I18n i18n;
	protected ProjectManager projectManager;
	protected RuntimeOptions runtimeOptions;
	protected Injector injector;

	@Override
	public void start(Stage stage) {
		runtimeOptions =new RuntimeOptions(null, null, null, null, false, false);
		ProB2Module module = new ProB2Module(new ProB2(), runtimeOptions);
		injector = Guice.createInjector(com.google.inject.Stage.PRODUCTION, module);
		i18n = injector.getInstance(I18n.class);
	}

}


