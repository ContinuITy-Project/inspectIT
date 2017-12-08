package rocks.inspectit.server.zipkin;

import java.sql.Timestamp;
import java.util.Map;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.collections.Transformer;

import io.opentracing.tag.Tags;
import rocks.inspectit.shared.all.tracing.constants.ExtraTags;
import rocks.inspectit.shared.all.tracing.data.AbstractSpan;
import rocks.inspectit.shared.all.tracing.data.ClientSpan;
import rocks.inspectit.shared.all.tracing.data.PropagationType;
import rocks.inspectit.shared.all.tracing.data.ServerSpan;
import rocks.inspectit.shared.all.tracing.data.SpanIdent;
import zipkin.BinaryAnnotation;
import zipkin2.Span.Kind;

/**
 * Span transformer knows to to translate opentracing.io based {@link zipkin2.Span} to the
 * {@link AbstractSpan}.
 *
 * @author Tobias Angerstein
 *
 */
public final class ZipkinSpanTransformer implements Transformer {

	private static final String HTTP_URL = "http.url";
	/**
	 * Instance for usage.
	 */
	public static final ZipkinSpanTransformer INSTANCE = new ZipkinSpanTransformer();

	/**
	 * Private, use {@link #INSTANCE} or {@link #transformSpan(SpanImpl)}.
	 */
	private ZipkinSpanTransformer() {
	}

	/**
	 * Transforms the opentracing.io span V! to our internal representation.
	 *
	 * @param zipkinSpan
	 *            {@link SpanImpl}.
	 * @return {@link AbstractSpan}.
	 */
	public static AbstractSpan transformSpan(Object zipkinSpan) {
		// check not null
		if (null == zipkinSpan) {
			return null;
		}
		if (zipkinSpan instanceof zipkin.Span) {
			return transformSpan((zipkin.Span) zipkinSpan);
		} else if (zipkinSpan instanceof zipkin2.Span) {
			if (null != ((zipkin2.Span) zipkinSpan).name()) {
				return transformSpan((zipkin2.Span) zipkinSpan);
			} else {
				return null;
			}
		} else {
			return null;
		}
	}

	/**
	 * Transforms the opentracing.io span V2 to our internal representation.
	 *
	 * @param zipkinSpan
	 *            {@link SpanImpl}.
	 * @return {@link AbstractSpan}.
	 */
	public static AbstractSpan transformSpan(zipkin2.Span zipkinSpan) {
		try {
			// context to ident
			SpanIdent ident = new SpanIdent(Long.parseLong(zipkinSpan.id(), 16), Long.parseLong(zipkinSpan.traceId(), 16));
			AbstractSpan span = createCorrectSpanType(zipkinSpan);
			span.setPropagationType(PropagationType.ZIPKIN);
			span.setSpanIdent(ident);

			// transform to inspectIT way of time handling
			if (null != zipkinSpan.timestamp()) {
				span.setTimeStamp(new Timestamp(zipkinSpan.timestamp() / 1000));
			} else {
				span.setTimeStamp(new Timestamp(0));
			}
			if (null != zipkinSpan.duration()) {
				span.setDuration(zipkinSpan.duration() / 1000.0d);
			} else {
				span.setDuration(-1);
			}
			// reference
			if (zipkinSpan.parentId() == null) {
				span.setParentSpanId(0);
			} else if (Long.parseLong(zipkinSpan.parentId(), 16) != ident.getId()) {
				span.setParentSpanId(Long.parseLong(zipkinSpan.parentId(), 16));
			}
			// span.setReferenceType(zipkinSpan.context().getReferenceType());

			// operation name (we save as tag)
			if (zipkinSpan.tags().containsKey(HTTP_URL)) {
				span.addTag(ExtraTags.OPERATION_NAME, zipkinSpan.localServiceName() + ":" + zipkinSpan.tags().get(HTTP_URL));
			}

			// tags
			if (MapUtils.isNotEmpty(zipkinSpan.tags())) {
				// TODO set propagation type
				for (Map.Entry<String, String> entry : zipkinSpan.tags().entrySet()) {
					if (!isTagIgnored(entry.getKey())) {
						span.addTag(entry.getKey(), entry.getValue());
					}
				}
			}

			// TODO what do we do about log data
			// we could add that to the ParameterContentData

			return span;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw e;
		}
	}

	/**
	 * Transforms the opentracing.io span to our internal representation.
	 *
	 * @param zipkinSpan
	 *            {@link SpanImpl}.
	 * @return {@link AbstractSpan}.
	 */
	public static AbstractSpan transformSpan(zipkin.Span zipkinSpan) {

		// context to ident
		SpanIdent ident = new SpanIdent(zipkinSpan.id, zipkinSpan.traceId);

		AbstractSpan span = createCorrectSpanType(zipkinSpan);
		span.setSpanIdent(ident);
		span.setPropagationType(PropagationType.ZIPKIN);

		// transform to inspectIT way of time handling
		long timestampMillis = zipkinSpan.timestamp / 1000;
		double durationMillis = zipkinSpan.duration / 1000.0d;
		span.setTimeStamp(new Timestamp(timestampMillis));
		span.setDuration(durationMillis);

		// reference
		if (null != zipkinSpan.parentId && zipkinSpan.parentId != ident.getId()) {
			span.setParentSpanId(zipkinSpan.parentId);
		}
		// span.setReferenceType(zipkinSpan.context().getReferenceType());

		String operationName = "";

		// operation name (we save as tag)
		if (null != zipkinSpan.binaryAnnotations) {
			for (BinaryAnnotation annotation : zipkinSpan.binaryAnnotations) {
				if (annotation.endpoint.serviceName != null) {
					operationName += annotation.endpoint.serviceName;
					break;
				}
			}
			if (null != zipkinSpan.name) {
				operationName += " (" + zipkinSpan.name + ")";
			}
			span.addTag(ExtraTags.OPERATION_NAME, operationName);
		}

		// TODO what do we do about log data
		// we could add that to the ParameterContentData

		return span;
	}

	/**
	 * Creates {@link ClientSpan} or {@link ServerSpan} based on the information from the given
	 * {@link zipkin.Span}.
	 *
	 * @param spanImpl
	 *            opentracing span impl
	 * @return {@link AbstractSpan}
	 */
	private static AbstractSpan createCorrectSpanType(zipkin2.Span zipkinSpan) {
		if (Kind.CLIENT.equals(zipkinSpan.kind())) {
			ClientSpan clientSpan = new ClientSpan();
			return clientSpan;
		} else {
			ServerSpan serverSpan = new ServerSpan();
			return serverSpan;
		}
	}

	/**
	 * Creates {@link ClientSpan} or {@link ServerSpan} based on the information from the given
	 * {@link zipkin.Span}.
	 *
	 * @param spanImpl
	 *            opentracing span impl
	 * @return {@link AbstractSpan}
	 */
	private static AbstractSpan createCorrectSpanType(zipkin.Span zipkinSpan) {
		return new ClientSpan();
	}

	/**
	 * If tags key is ignored for the copy to the our span representation. Ignored are: propagation
	 * type. method and sensor id as well as spin kind.
	 *
	 * @param key
	 *            tag key
	 * @return if tag with given key should be ignored when copied.
	 */
	private static boolean isTagIgnored(String key) {
		return ExtraTags.PROPAGATION_TYPE.equals(key) || ExtraTags.INSPECTT_METHOD_ID.equals(key) || ExtraTags.INSPECTT_SENSOR_ID.equals(key) || Tags.SPAN_KIND.getKey().equals(key);
	}

	@Override
	public Object transform(Object input) {
		// TODO Auto-generated method stub
		return null;
	}
}
