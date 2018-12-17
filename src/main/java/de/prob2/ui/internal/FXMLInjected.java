package de.prob2.ui.internal;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the annotated class should be injected using Guice if it is used in an FXML file, instead of using the default JavaFX builder mechanism.
 * This is needed for our custom JavaFX view subclasses, which have to be created through Guice for the dependency injection to work properly.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface FXMLInjected {}
