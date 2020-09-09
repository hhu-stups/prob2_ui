package de.prob2.ui.internal;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;

public final class BasicConfigModule extends AbstractModule {
	public BasicConfigModule() {
		super();
	}
	
	@Override
	protected void configure() {
		install(new DataPathsModule());
	}
	
	@Provides
	private static Gson provideGson() {
		return new GsonBuilder().create();
	}
}
