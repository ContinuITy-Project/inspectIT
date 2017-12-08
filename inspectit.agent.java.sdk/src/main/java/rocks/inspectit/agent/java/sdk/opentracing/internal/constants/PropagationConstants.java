package rocks.inspectit.agent.java.sdk.opentracing.internal.constants;

/**
 * Propagation constants.
 *
 * @author Ivan Senic
 *
 */
public interface PropagationConstants {

	/**
	 * Prefix for the propagation baggage.
	 */
	String INSPECTIT_PREFIX = "X-B3-";

	/**
	 * Header name for the span id.
	 */
	String SPAN_ID = INSPECTIT_PREFIX + "SpanId";

	/**
	 * Header name for the parent id.
	 */
	String PARENT_ID = INSPECTIT_PREFIX + "ParentId";

	/**
	 * Header name for the trace id.
	 */
	String TRACE_ID = INSPECTIT_PREFIX + "TraceId";

	/**
	 * Prefix for the propagation baggage.
	 */
	String INSPECTIT_BAGGAGE_PREFIX = INSPECTIT_PREFIX + "baggage_";

	String SPAN_ID_LOW = INSPECTIT_PREFIX + "Spanid";

	String TRACE_ID_LOW = INSPECTIT_PREFIX + "Traceid";

	String PARENT_ID_LOW = INSPECTIT_PREFIX + "Parentid";
}
