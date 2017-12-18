package rocks.inspectit.server.open.xtrace;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.Version;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.module.SimpleModule;
import org.spec.research.open.xtrace.adapters.inspectit.impl.IITHTTPRequestProcessing;
import org.spec.research.open.xtrace.adapters.inspectit.impl.IITRemoteInvocation;
import org.spec.research.open.xtrace.adapters.inspectit.impl.IITSpanCallable;
import org.spec.research.open.xtrace.adapters.inspectit.impl.IITSubTraceImpl;
import org.spec.research.open.xtrace.adapters.inspectit.impl.IITTraceImpl;
import org.spec.research.open.xtrace.api.core.Trace;

/**
 * OPEN.xtrace serializer.
 * 
 * @author Tobias Angerstein
 *
 */
public class OPENxtraceSerializer {

	ObjectMapper objectMapper;

	public OPENxtraceSerializer() {
		createObjectMapper();
	}

	/**
	 * Serializes OPEN.xtrace into JSON.
	 * 
	 * @param traceImplList
	 *            list of traces
	 * @throws JsonGenerationException
	 * @throws JsonMappingException
	 * @throws IOException
	 * @return the JSON string
	 */
	public String serialize(List<Trace> traceImplList) {
		try {
			return objectMapper.writeValueAsString(traceImplList);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Creates Object Mapper and registers Serializers
	 */
	private void createObjectMapper() {
		objectMapper = new ObjectMapper();

		SimpleModule serializationModule = new SimpleModule("", new Version(2, 3, 2, "Serialization open.XTRACE"));
		serializationModule.addSerializer(IITSpanCallable.class, new IITSpanCallableSerializer());
		serializationModule.addSerializer(IITTraceImpl.class, new IITTraceImplSerializer());
		serializationModule.addSerializer(IITSubTraceImpl.class, new IITSubTraceImplSerializer());
		serializationModule.addSerializer(IITRemoteInvocation.class, new IITRemoteInvocationSerializer());
		serializationModule.addSerializer(IITHTTPRequestProcessing.class, new IITHTTPRequestProcessingSerializer());

		objectMapper.registerModule(serializationModule);
	}

	// Serializers

	/**
	 * Serializes {@link IITSpanCallable}
	 * 
	 * @author Tobias Angerstein
	 *
	 */
	class IITSpanCallableSerializer extends JsonSerializer<IITSpanCallable> {

		public IITSpanCallableSerializer() {
			super();
		}

		@Override
		public void serialize(IITSpanCallable value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
			jgen.writeStartObject();
			jgen.writeNumberField("timeStamp", value.getTimestamp());
			jgen.writeNumberField("usecaseID", value.getUseCaseID().orElse(-1l));
			jgen.writeObjectField("identifier", (UUID) value.getIdentifier().orElse("not available"));
			jgen.writeStringField("usecaseName", value.getUseCaseName().orElse("not available"));
			jgen.writeStringField("sessionCookie", value.getSessionCookie().orElse(""));
			jgen.writeObjectField("children", value.getCallees());
			jgen.writeObjectField("invocationSequence", value.getInvocationSequenceData());
			jgen.writeStringField("@class", IITSpanCallable.class.getCanonicalName());
			jgen.writeEndObject();
		}
	}

	/**
	 * Serializes {@link IITTraceImpl}
	 * 
	 * @author Tobias Angerstein
	 *
	 */
	class IITTraceImplSerializer extends JsonSerializer<IITTraceImpl> {

		public IITTraceImplSerializer() {
			super();
		}

		@Override
		public void serialize(IITTraceImpl value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
			jgen.writeStartObject();
			jgen.writeObjectField("rootOfTrace", value.getRoot());
			jgen.writeNumberField("traceId", value.getTraceId());
			jgen.writeObjectField("exclusiveTime", value.getExclusiveTime());
			jgen.writeNumberField("identifier", (Long) value.getIdentifier().get());
			jgen.writeStringField("@class", IITTraceImpl.class.getCanonicalName());
			jgen.writeEndObject();
		}
	}

	/**
	 * Serializes {@link IITSubTraceImpl}
	 * 
	 * @author Tobias Angerstein
	 *
	 */
	class IITSubTraceImplSerializer extends JsonSerializer<IITSubTraceImpl> {

		public IITSubTraceImplSerializer() {
			super();
		}

		@Override
		public void serialize(IITSubTraceImpl value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
			jgen.writeStartObject();
			jgen.writeObjectField("rootOfSubtrace", value.getRoot());
			jgen.writeObjectField("platformIdent", value.getPlatformIdent());
			jgen.writeNumberField("identifier", (Long) value.getIdentifier().get());
			jgen.writeObjectField("invocationSequence", value.getInvocationSequenceData());
			jgen.writeStringField("serverName", value.getServerName().orElse("not available"));
			jgen.writeStringField("@class", IITSubTraceImpl.class.getCanonicalName());
			jgen.writeEndObject();
		}
	}

	/**
	 * Serializes {@link IITRemoteInvocation}
	 * 
	 * @author Tobias Angerstein
	 *
	 */
	class IITRemoteInvocationSerializer extends JsonSerializer<IITRemoteInvocation> {

		public IITRemoteInvocationSerializer() {
			super();
		}

		@Override
		public void serialize(IITRemoteInvocation value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
			jgen.writeStartObject();
			jgen.writeNumberField("timeStamp", value.getTimestamp());
			jgen.writeObjectField("targetSubTrace", value.getTargetSubTrace().orElse(null));
			jgen.writeObjectField("invocationSequence", value.getInvocationSequenceData());
			jgen.writeStringField("@class", IITRemoteInvocation.class.getCanonicalName());
			jgen.writeEndObject();
		}
	}

	/**
	 * Serializes {@link IITHTTPRequestProcessing}
	 * 
	 * @author Tobias Angerstein
	 *
	 */
	class IITHTTPRequestProcessingSerializer extends JsonSerializer<IITHTTPRequestProcessing> {

		public IITHTTPRequestProcessingSerializer() {
			super();
		}

		@Override
		public void serialize(IITHTTPRequestProcessing value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
			jgen.writeStartObject();
			jgen.writeNumberField("timeStamp", value.getTimestamp());
			jgen.writeObjectField("invocationSequence", value.getInvocationSequenceData());
			jgen.writeStringField("@class", IITHTTPRequestProcessing.class.getCanonicalName());
			jgen.writeEndObject();
		}
	}
}
