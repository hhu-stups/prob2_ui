package de.prob2.ui.internal;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
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
	private static ObjectMapper provideObjectMapper() {
		final ObjectMapper objectMapper = new ObjectMapper();
		// During basic config loading,
		// we only want to read some of the config fields,
		// so it's expected that there are "unknown" properties.
		objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
		return objectMapper;
	}
}
