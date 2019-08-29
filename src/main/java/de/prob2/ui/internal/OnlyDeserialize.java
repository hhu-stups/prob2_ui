package de.prob2.ui.internal;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.google.gson.Gson;

/**
 * Indicates that a field should be deserialized, but not serialized, by our custom {@link Gson} instance. This can be used to read obsolete fields from old files, without keeping the field when the file is saved again.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface OnlyDeserialize {}
