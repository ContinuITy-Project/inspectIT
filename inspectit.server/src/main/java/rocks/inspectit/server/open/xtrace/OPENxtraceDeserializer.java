package rocks.inspectit.server.open.xtrace;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.Version;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonDeserializer;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.module.SimpleModule;
import org.spec.research.open.xtrace.adapters.inspectit.impl.IITAbstractCallable;
import org.spec.research.open.xtrace.adapters.inspectit.impl.IITAbstractNestingCallable;
import org.spec.research.open.xtrace.adapters.inspectit.impl.IITHTTPRequestProcessing;
import org.spec.research.open.xtrace.adapters.inspectit.impl.IITRemoteInvocation;
import org.spec.research.open.xtrace.adapters.inspectit.impl.IITSpanCallable;
import org.spec.research.open.xtrace.adapters.inspectit.impl.IITSubTraceImpl;
import org.spec.research.open.xtrace.adapters.inspectit.impl.IITTraceImpl;
import org.spec.research.open.xtrace.api.core.SubTrace;
import org.spec.research.open.xtrace.api.core.callables.Callable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import rocks.inspectit.shared.all.communication.data.InvocationSequenceData;

/**
 * OPEN.xtrace deserializer.
 * 
 * @author Tobias Angerstein
 *
 */
public class OPENxtraceDeserializer {

	ObjectMapper objectMapper;

	public OPENxtraceDeserializer() {
		createObjectMapper();
	}

	/**
	 * Sets missing cyclical references, which could not be serialized.
	 * 
	 * @param traceImpl
	 *            trace.
	 * @return trace including cyclical references.
	 */
	private IITTraceImpl setMissingReferences(IITTraceImpl traceImpl) {
		traceImpl.getRoot().setContainingTrace(traceImpl);
		IITSubTraceImpl subTraceImpl = (IITSubTraceImpl) traceImpl.getRoot();
		setMissingReferencesInCallable((IITAbstractCallable) subTraceImpl.getRoot(), null, subTraceImpl);
		return traceImpl;
	}

	/**
	 * Set missing references in callable.
	 * 
	 * @param callable
	 *            current callable
	 * @param parent
	 *            parent callable
	 * @param subTraceImpl
	 *            containing sub trace
	 */
	private void setMissingReferencesInCallable(Callable callable, IITAbstractNestingCallable parent, IITSubTraceImpl subTraceImpl) {
		if (callable instanceof IITAbstractCallable) {
			((IITAbstractCallable) callable).setContainingSubTrace((SubTrace) subTraceImpl);
			((IITAbstractCallable) callable).setParent(parent);
		}
		if (callable instanceof IITAbstractNestingCallable) {
			for (Callable childCallable : ((IITAbstractNestingCallable) callable).getCallees()) {
				setMissingReferencesInCallable(childCallable, ((IITAbstractNestingCallable) callable), subTraceImpl);
			}
		}
	}

	/**
	 * Deserializes JSON into a list of {@link IITTraceImpl}.
	 * 
	 * @param serializedTrace
	 *            JSON string
	 * @return a @{@link ArrayList} containing {@link IITTraceImpl}}
	 * @throws JsonGenerationException
	 * @throws JsonMappingException
	 * @throws IOException
	 */
	public List<IITTraceImpl> deserialize(String serializedTrace) throws JsonGenerationException, JsonMappingException, IOException {
		com.google.gson.JsonParser parser = new com.google.gson.JsonParser();
		JsonElement tradeElement = parser.parse(serializedTrace);
		JsonArray jsonTraceArray = tradeElement.getAsJsonArray();

		ArrayList<IITTraceImpl> resultList = new ArrayList<IITTraceImpl>();

		for (JsonElement element : jsonTraceArray) {
			resultList.add(setMissingReferences(objectMapper.readValue(element.toString(), IITTraceImpl.class)));
		}

		return resultList;
	}

	/**
	 * Creates Object Mapper and registers Serializers.
	 */
	private void createObjectMapper() {
		objectMapper = new ObjectMapper();

		SimpleModule deserializationModule = new SimpleModule("", new Version(2, 3, 2, "Deserialization open.XTRACE"));
		deserializationModule.addDeserializer(IITSpanCallable.class, new IITSpanCallableDeserializer());
		deserializationModule.addDeserializer(IITTraceImpl.class, new IITTraceImplDeserializer());
		deserializationModule.addDeserializer(IITSubTraceImpl.class, new IITSubTraceImplDeserializer());
		deserializationModule.addDeserializer(IITRemoteInvocation.class, new IITRemoteInvocationDeserializer());
		deserializationModule.addDeserializer(IITHTTPRequestProcessing.class, new IITHTTPRequestProcessingDeserializer());

		objectMapper.registerModule(deserializationModule);
	}

	// Deserializer

	/**
	 * Deserializes {@link IITSpanCallable}
	 * 
	 * @author Tobias Angerstein
	 *
	 */
	class IITSpanCallableDeserializer extends JsonDeserializer<IITSpanCallable> {

		public IITSpanCallableDeserializer() {
			super();
		}

		@Override
		public IITSpanCallable deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
			JsonNode node = jp.getCodec().readTree(jp);
			InvocationSequenceData invocationSequence = objectMapper.treeToValue(node.get("invocationSequence"), InvocationSequenceData.class);
			UUID identifier = objectMapper.treeToValue(node.get("identifier"), UUID.class);
			IITSubTraceImpl containingTrace = null;
			IITSpanCallable parent = null;
			List<Callable> children = new ArrayList<Callable>();
			if (null != node.get("children")) {
				for (JsonNode callableJson : node.get("children")) {
					if (callableJson.get("@class").asText().equals("org.spec.research.open.xtrace.adapters.inspectit.impl.IITRemoteInvocation")) {
						children.add(objectMapper.treeToValue(callableJson, IITRemoteInvocation.class));
					} else if (callableJson.get("@class").asText().equals("org.spec.research.open.xtrace.adapters.inspectit.impl.IITHTTPRequestProcessing")) {
						children.add(objectMapper.treeToValue(callableJson, IITHTTPRequestProcessing.class));
					} else {
						children.add(objectMapper.treeToValue(callableJson, IITSpanCallable.class));
					}
				}
			}
			try {
				return new IITSpanCallable(containingTrace, identifier, parent, node.get("usecaseID").asLong(), node.get("usecaseName").asText(), node.get("sessionCookie").asText(), invocationSequence, children);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return null;
			}
		}
	}

	/**
	 * Deserializes {@link IITTraceImpl}
	 * 
	 * @author Tobias Angerstein
	 *
	 */
	class IITTraceImplDeserializer extends JsonDeserializer<IITTraceImpl> {

		public IITTraceImplDeserializer() {
			super();
		}

		@Override
		public IITTraceImpl deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
			JsonNode node = jp.getCodec().readTree(jp);
			Long identifier = node.get("identifier").asLong();
			IITSubTraceImpl subTrace = objectMapper.treeToValue(node.get("rootOfTrace"), IITSubTraceImpl.class);
			return new IITTraceImpl(identifier, subTrace);
		}
	}

	/**
	 * Deserializes {@link IITSubTraceImpl}
	 * 
	 * @author Tobias Angerstein
	 *
	 */
	class IITSubTraceImplDeserializer extends JsonDeserializer<IITSubTraceImpl> {

		public IITSubTraceImplDeserializer() {
			super();
		}

		@Override
		public IITSubTraceImpl deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
			JsonNode node = jp.getCodec().readTree(jp);
			IITTraceImpl containingTrace = null;
			Long identifier = null;
			try {
				identifier = node.get("identifier").asLong();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Callable callable = null;
			if (node.get("rootOfSubtrace").get("@class").toString().equals("org.spec.research.open.xtrace.adapters.inspectit.impl.IITRemoteInvocation")) {
				callable = objectMapper.treeToValue(node.get("rootOfSubtrace"), IITRemoteInvocation.class);
			} else {
				callable = objectMapper.treeToValue(node.get("rootOfSubtrace"), IITSpanCallable.class);
			}
			return new IITSubTraceImpl(containingTrace, identifier, callable);
		}
	}

	/**
	 * Deserializes {@link IITRemoteInvocation}
	 * 
	 * @author Tobias Angerstein
	 *
	 */
	class IITRemoteInvocationDeserializer extends JsonDeserializer<IITRemoteInvocation> {

		public IITRemoteInvocationDeserializer() {
			super();
		}

		@Override
		public IITRemoteInvocation deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
			JsonNode node = jp.getCodec().readTree(jp);
			InvocationSequenceData invocationSequence = objectMapper.treeToValue(node.get("invocationSequence"), InvocationSequenceData.class);
			IITSubTraceImpl containingTrace = null;
			IITSpanCallable parent = null;
			IITRemoteInvocation remoteInvocation = new IITRemoteInvocation(invocationSequence, containingTrace, parent);
			remoteInvocation.setTargetSubTrace(objectMapper.treeToValue(node.get("targetSubTrace"), IITSubTraceImpl.class));
			return remoteInvocation;
		}
	}

	/**
	 * Deserializes {@link IITHTTPRequestProcessing}
	 * 
	 * @author Tobias Angerstein
	 *
	 */
	class IITHTTPRequestProcessingDeserializer extends JsonDeserializer<IITHTTPRequestProcessing> {

		public IITHTTPRequestProcessingDeserializer() {
			super();
		}

		@Override
		public IITHTTPRequestProcessing deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
			JsonNode node = jp.getCodec().readTree(jp);
			InvocationSequenceData invocationSequence = objectMapper.treeToValue(node.get("invocationSequence"), InvocationSequenceData.class);
			IITSubTraceImpl containingTrace = null;
			IITSpanCallable parent = null;
			IITHTTPRequestProcessing httpRequestProcessing = new IITHTTPRequestProcessing(invocationSequence, containingTrace, parent);
			return httpRequestProcessing;
		}
	}
}
