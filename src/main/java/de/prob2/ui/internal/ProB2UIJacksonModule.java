package de.prob2.ui.internal;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import com.fasterxml.jackson.databind.ext.NioPathSerializer;
import com.fasterxml.jackson.databind.module.SimpleDeserializers;
import com.fasterxml.jackson.databind.module.SimpleSerializers;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import javafx.geometry.BoundingBox;

/**
 * Jackson module containing custom serializers and deserializers used by the UI.
 */
final class ProB2UIJacksonModule extends Module {
	// Jackson already provides default (de)serializers for Path by default,
	// but their serialization format doesn't match our previous Gson-based code.
	// In particular, Jackson's default Path serializer always outputs file:// URLs
	// and has no way to serialize relative paths as-is
	// (the serializer always converts them to absolute paths).
	// So we provide our own Path (de)serializer implementations
	// that use the same serialization format as we used before
	// and that support serializting relative paths.
	
	// Extend the standard NioPathSerializer to inherit the implementation of serializeWithType.
	private static final class CustomNioPathSerializer extends NioPathSerializer {
		private static final long serialVersionUID = 1L;
		
		private CustomNioPathSerializer() {
			super();
		}
		
		@Override
		public void serialize(final Path value, final JsonGenerator gen, final SerializerProvider serializers) throws IOException {
			gen.writeString(value.toString().replaceAll("\\\\", "/"));
		}
	}
	
	private static final class CustomNioPathDeserializer extends StdScalarDeserializer<Path> {
		private static final long serialVersionUID = 1L;
		
		private CustomNioPathDeserializer() {
			super(Path.class);
		}
		
		@Override
		public Path deserialize(final JsonParser p, final DeserializationContext ctxt) throws IOException {
			if (!p.hasToken(JsonToken.VALUE_STRING)) {
				return (Path) ctxt.handleUnexpectedToken(Path.class, p);
			}
			
			return Paths.get(p.getText());
		}
	}
	
	// (De)serializers for JavaFX BoundingBox objects,
	// using the same array-based serialization format as our previous Gson-based code.
	// The 3D fields of BoundingBox (minZ and depth) are ignored,
	// because we only use 2D BoundingBoxes.
	
	private static final class BoundingBoxSerializer extends StdSerializer<BoundingBox> {
		private static final long serialVersionUID = 1L;
		
		private BoundingBoxSerializer() {
			super(BoundingBox.class);
		}
		
		@Override
		public void serialize(final BoundingBox value, final JsonGenerator gen, final SerializerProvider provider) throws IOException {
			gen.writeStartArray(value, 4);
			gen.writeNumber(value.getMinX());
			gen.writeNumber(value.getMinY());
			gen.writeNumber(value.getWidth());
			gen.writeNumber(value.getHeight());
			gen.writeEndArray();
		}
	}
	
	private static final class BoundingBoxDeserializer extends StdDeserializer<BoundingBox> {
		private static final long serialVersionUID = 1L;
		
		private BoundingBoxDeserializer() {
			super(BoundingBox.class);
		}
		
		private static double readDouble(final JsonParser p, final DeserializationContext ctxt) throws IOException {
			final JsonToken token = p.nextToken();
			if (!token.isNumeric()) {
				return (double)ctxt.handleUnexpectedToken(BoundingBox.class, p);
			}
			return p.getDoubleValue();
		}
		
		@Override
		public BoundingBox deserialize(final JsonParser p, final DeserializationContext ctxt) throws IOException {
			if (!p.hasToken(JsonToken.START_ARRAY)) {
				return (BoundingBox)ctxt.handleUnexpectedToken(BoundingBox.class, p);
			}
			p.nextToken();
			final double minX = ctxt.readValue(p, double.class);
			p.nextToken();
			final double minY = ctxt.readValue(p, double.class);
			p.nextToken();
			final double width = ctxt.readValue(p, double.class);
			p.nextToken();
			final double height = ctxt.readValue(p, double.class);
			if (p.nextToken() != JsonToken.END_ARRAY) {
				return (BoundingBox)ctxt.handleUnexpectedToken(BoundingBox.class, p);
			}
			
			return new BoundingBox(minX, minY, width, height);
		}
	}
	
	ProB2UIJacksonModule() {
		super();
	}
	
	@Override
	public String getModuleName() {
		return "ProB 2 UI Jackson module";
	}
	
	@Override
	public Version version() {
		return Version.unknownVersion();
	}
	
	@Override
	public void setupModule(final Module.SetupContext context) {
		context.addSerializers(new SimpleSerializers(Arrays.asList(
			new CustomNioPathSerializer(),
			new BoundingBoxSerializer()
		)));
		
		final Map<Class<?>, JsonDeserializer<?>> deserializers = new HashMap<>();
		deserializers.put(Path.class, new CustomNioPathDeserializer());
		deserializers.put(BoundingBox.class, new BoundingBoxDeserializer());
		context.addDeserializers(new SimpleDeserializers(deserializers));
	}
}
