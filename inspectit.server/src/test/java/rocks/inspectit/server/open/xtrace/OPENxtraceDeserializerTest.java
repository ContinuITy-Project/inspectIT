package rocks.inspectit.server.open.xtrace;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.mockito.Mock;
import org.spec.research.open.xtrace.adapters.inspectit.impl.IITHTTPRequestProcessing;
import org.spec.research.open.xtrace.adapters.inspectit.impl.IITRemoteInvocation;
import org.spec.research.open.xtrace.adapters.inspectit.impl.IITSpanCallable;
import org.spec.research.open.xtrace.adapters.inspectit.impl.IITSubTraceImpl;
import org.spec.research.open.xtrace.adapters.inspectit.impl.IITTraceImpl;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import rocks.inspectit.shared.all.communication.data.HttpTimerData;
import rocks.inspectit.shared.all.communication.data.InvocationSequenceData;
import rocks.inspectit.shared.all.testbase.TestBase;

/**
 * Tests the {@link OPENxtraceDeserializer} class.
 * 
 * @author Tobias Angerstein
 *
 */
public class OPENxtraceDeserializerTest extends TestBase {

	OPENxtraceDeserializer deserializer = new OPENxtraceDeserializer();

	IITTraceImpl trace;

	IITSubTraceImpl subTrace;

	@Mock
	IITSpanCallable spanCallable;

	IITRemoteInvocation remoteInvocation;

	IITHTTPRequestProcessing httpRequestProcessing;

	/**
	 * Tests the {@link OPENxtraceSerializer#serialize(java.util.List)} method.
	 * 
	 * @author Tobias Angerstein
	 *
	 */
	public static class Deserialize extends OPENxtraceDeserializerTest {

		private static final String OPEN_XTRACE_JSON_STRING = "[{\"rootOfTrace\":{\"rootOfSubtrace\":{\"timeStamp\":23452345,\"usecaseID\":345345,\"identifier\":\"5b2a270d-716f-4def-8f99-c66bcfaf0193\","
				+ "\"usecaseName\":\"test\",\"sessionCookie\":\"cookie\",\"children\":[{\"timeStamp\":23452345,\"targetSubTrace\":null,\"invocationSequence\":{\"id\":12,\"platformIdent\":0,\"sensorTypeIdent\":0,"
				+ "\"timeStamp\":23452345,\"methodIdent\":0,\"timerData\":{\"@class\":\"rocks.inspectit.shared.all.communication.data.HttpTimerData\",\"id\":0,\"platformIdent\":34,\"sensorTypeIdent\":23,\"timeStamp"
				+ "\":23452345,\"methodIdent\":0,\"min\":-1.0,\"max\":-1.0,\"count\":0,\"duration\":0.0,\"cpuMin\":-1.0,\"cpuMax\":-1.0,\"cpuDuration\":0.0,\"exclusiveCount\":0,\"exclusiveDuration\":0.0,\"exclusiveMax"
				+ "\":-1.0,\"exclusiveMin\":-1.0,\"parameters\":null,\"attributes\":null,\"headers\":null,\"sessionAttributes\":null,\"httpResponseStatus\":0,\"httpInfo\":{\"uri\":\"n.a.\",\"scheme\":null,\"serverName\":null,"
				+ "\"serverPort\":null,\"queryString\":null,\"requestMethod\":\"n.a.\"}},\"duration\":6.7856757E7,\"childCount\":0},\"@class\":\"org.spec.research.open.xtrace.adapters.inspectit.impl.IITRemoteInvocation\"},{\"timeStamp\":23452345,"
				+ "\"invocationSequence\":{\"id\":12,\"platformIdent\":0,\"sensorTypeIdent\":0,\"timeStamp\":23452345,\"methodIdent\":0,\"timerData\":{\"@class\":\"rocks.inspectit.shared.all.communication.data.HttpTimerData\","
				+ "\"id\":0,\"platformIdent\":34,\"sensorTypeIdent\":23,\"timeStamp\":23452345,\"methodIdent\":0,\"min\":-1.0,\"max\":-1.0,\"count\":0,\"duration\":0.0,\"cpuMin\":-1.0,\"cpuMax\":-1.0,\"cpuDuration\":0.0,\"exclusiveCount"
				+ "\":0,\"exclusiveDuration\":0.0,\"exclusiveMax\":-1.0,\"exclusiveMin\":-1.0,\"parameters\":null,\"attributes\":null,\"headers\":null,\"sessionAttributes\":null,\"httpResponseStatus\":0,\"httpInfo\":{"
				+ "\"uri\":\"n.a.\",\"scheme\":null,\"serverName\":null,\"serverPort\":null,\"queryString\":null,\"requestMethod\":\"n.a.\"}},\"duration\":6.7856757E7,\"childCount\":0},\"@class\":"
				+ "\"org.spec.research.open.xtrace.adapters.inspectit.impl.IITHTTPRequestProcessing\"}],\"invocationSequence\":{\"id\":12,\"platformIdent\":0,\"sensorTypeIdent\":0,\"timeStamp\":23452345,\"methodIdent\":0,\"timerData"
				+ "\":{\"@class\":\"rocks.inspectit.shared.all.communication.data.HttpTimerData\",\"id\":0,\"platformIdent\":34,\"sensorTypeIdent\":23,\"timeStamp\":23452345,\"methodIdent\":0,\"min\":-1.0,\"max\":-1.0,\"count\":0,"
				+ "\"duration\":0.0,\"cpuMin\":-1.0,\"cpuMax\":-1.0,\"cpuDuration\":0.0,\"exclusiveCount\":0,\"exclusiveDuration\":0.0,\"exclusiveMax\":-1.0,\"exclusiveMin\":-1.0,\"parameters\":null,\"attributes\":null,\"headers\":null,"
				+ "\"sessionAttributes\":null,\"httpResponseStatus\":0,\"httpInfo\":{\"uri\":\"n.a.\",\"scheme\":null,\"serverName\":null,\"serverPort\":null,\"queryString\":null,\"requestMethod\":\"n.a.\"}},\"duration\":6.7856757E7,\"childCount\":0},"
				+ "\"@class\":\"org.spec.research.open.xtrace.adapters.inspectit.impl.IITSpanCallable\"},\"platformIdent\":null,\"identifier\":879789,\"invocationSequence\":null,\"serverName\":\"not available\",\"@class\":"
				+ "\"org.spec.research.open.xtrace.adapters.inspectit.impl.IITSubTraceImpl\"},\"traceId\":-1,\"exclusiveTime\":67856757000000,\"identifier\":78678,\"@class\":\"org.spec.research.open.xtrace.adapters.inspectit.impl.IITTraceImpl\"}]";

		@BeforeTest
		public void prepareTestData() {
			InvocationSequenceData invocationSequenceData = new InvocationSequenceData();
			invocationSequenceData.setTimerData(new HttpTimerData(new Timestamp(23452345l), 34l, 23l, 0));
			invocationSequenceData.setDuration(67856757l);
			invocationSequenceData.setTimeStamp(new Timestamp(23452345l));
			invocationSequenceData.setId(12);
			remoteInvocation = new IITRemoteInvocation(invocationSequenceData, subTrace, spanCallable);
			remoteInvocation.setTargetSubTrace(null);
			httpRequestProcessing = new IITHTTPRequestProcessing(invocationSequenceData, subTrace, spanCallable);
			spanCallable = new IITSpanCallable(subTrace, null, 345345l, "test", "cookie", invocationSequenceData);
			spanCallable.setIdentifier(UUID.fromString("5b2a270d-716f-4def-8f99-c66bcfaf0193"));
			subTrace = new IITSubTraceImpl(trace, 879789l, spanCallable);
			trace = new IITTraceImpl(78678l, subTrace);
			remoteInvocation.setContainingSubTrace(subTrace);
			httpRequestProcessing.setContainingSubTrace(subTrace);
			remoteInvocation.setParent(spanCallable);
			httpRequestProcessing.setParent(spanCallable);
			subTrace.setContainingTrace(trace);
			spanCallable.setContainingSubTrace(subTrace);
			spanCallable.addChild(remoteInvocation);
			spanCallable.addChild(httpRequestProcessing);
		}

		@Test
		public void deserializeTestData() throws JsonGenerationException, JsonMappingException, IOException {
			List<IITTraceImpl> result = deserializer.deserialize(OPEN_XTRACE_JSON_STRING);
			try {
				assertThat(comp(trace, result.get(0), 0), is(0));
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		}

		private static List<Object> seen = new ArrayList<>();

		/**
		 * Compares two objects.
		 * 
		 * @param o1
		 *            first object
		 * @param o2
		 *            second object
		 * @param numberOfDifferences
		 *            the initial number of differences
		 * @return the total number of differences
		 * @throws IllegalAccessException
		 */
		private int comp(Object o1, Object o2, int numberOfDifferences) throws IllegalAccessException {
			if (o1 == null && o2 != null) {
				numberOfDifferences++;
			} else if (o1 != null && o2 == null) {
				numberOfDifferences++;
			}
			ArrayList<Field> fields = new ArrayList<Field>();

			getAllFields(fields, o1.getClass());

			for (Field f : fields) {
				f.setAccessible(true);
				Object r1 = f.get(o1);
				Object r2 = f.get(o2);
				if (r1 == null && r2 != null) {
					numberOfDifferences++;
					continue;
				} else if (r1 != null && r2 == null) {
					numberOfDifferences++;
					continue;
				} else if (r1 == null && r2 == null) {
					continue;
				}
				if (seen.contains(r1) || seen.contains(r2)) {
					continue;
				} else {
					seen.add(r1);
					seen.add(r2);
				}
				if (isPrimitiveOrPrimitiveWrapperOrString(r1.getClass())) {
					if (!r1.equals(r2)) {
						numberOfDifferences++;
					}
				} else {

					numberOfDifferences += comp(r1, r2, numberOfDifferences);
				}
			}
			return numberOfDifferences;
		}

		/**
		 * Returns all fields including the fields of the superclass.
		 * 
		 * @param fields
		 *            the list, which will be filled.
		 * @param type
		 *            the class type
		 * @return the filled list
		 */
		private static List<Field> getAllFields(List<Field> fields, Class<?> type) {
			fields.addAll(Arrays.asList(type.getDeclaredFields()));
			if (type.getSuperclass() != null) {
				getAllFields(fields, type.getSuperclass());
			}
			return fields;
		}

		/**
		 * Checks whether a type is a primitive
		 * 
		 * @param type
		 *            the class type
		 * @return is primitive
		 */
		public static boolean isPrimitiveOrPrimitiveWrapperOrString(Class<?> type) {
			return (type.isPrimitive() && type != void.class) || type == Double.class || type == Float.class || type == Long.class || type == Integer.class || type == Short.class || type == Character.class
					|| type == Byte.class || type == Boolean.class || type == String.class;
		}
	}

}