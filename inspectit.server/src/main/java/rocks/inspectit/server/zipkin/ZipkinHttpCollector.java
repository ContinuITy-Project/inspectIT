package rocks.inspectit.server.zipkin;

import static org.springframework.web.bind.annotation.RequestMethod.POST;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.zip.GZIPInputStream;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import rocks.inspectit.server.cache.IBuffer;
import rocks.inspectit.server.cache.impl.BufferElement;
import rocks.inspectit.server.service.rest.error.JsonError;
import rocks.inspectit.server.util.CacheIdGenerator;
import rocks.inspectit.shared.all.communication.DefaultData;
import rocks.inspectit.shared.all.tracing.data.AbstractSpan;
import zipkin.SpanDecoder;
import zipkin2.codec.SpanBytesDecoder;

/**
 * Implements the POST endpoints for zipkin tracers.
 */
@Controller
@RequestMapping(value = "/api")
public class ZipkinHttpCollector {
	/**
	 * inspectIT buffer
	 */
	@Autowired
	private IBuffer<DefaultData> buffer;

	static final String APPLICATION_THRIFT = "application/x-thrift";
	/**
	 * id generator.
	 */
	@Autowired
	private CacheIdGenerator idGenerator;

	/**
	 * 201 response.
	 */
	static final ResponseEntity<HttpStatus> SUCCESS = new ResponseEntity<>(HttpStatus.ACCEPTED);

	/**
	 * 400 response.
	 */
	static final ResponseEntity<HttpStatus> FAILURE = new ResponseEntity<>(HttpStatus.BAD_REQUEST);

	/**
	 * Rest interface for V2 spans.
	 * 
	 * @param encoding
	 * @param body
	 * @return
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	@RequestMapping(value = "/v2/spans", method = POST)
	@ResponseBody
	public ResponseEntity<HttpStatus> uploadSpansJson2(@RequestHeader(value = "Content-Encoding", required = false) String encoding, @RequestBody byte[] body) throws InterruptedException, ExecutionException {
		return validateAndStoreSpans(encoding, body, InputType.V2);
	}

	/**
	 * Rest interface for V1 spans.
	 * 
	 * @param encoding
	 * @param body
	 * @return
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	@RequestMapping(value = "/v1/spans", method = POST, consumes = APPLICATION_THRIFT)
	@ResponseBody
	public ResponseEntity<HttpStatus> uploadSpansJsonThrift(@RequestHeader(value = "Content-Encoding", required = false) String encoding, @RequestBody byte[] body) throws InterruptedException, ExecutionException {
		return validateAndStoreSpans(encoding, body, InputType.V1_THRIFT);
	}

	/**
	 * Validates and stores all incoming spans.
	 * 
	 * @param encoding
	 * @param body
	 * @param inputType
	 * @return
	 */
	ResponseEntity<HttpStatus> validateAndStoreSpans(String encoding, byte[] body, InputType inputType) {
		if (encoding != null && encoding.contains("gzip")) {
			try {
				body = gunzip(body);
			} catch (IOException e) {
				return FAILURE;
			}
		}
		if (inputType.equals(InputType.V1)) {
			List<zipkin.Span> spans = decodeV1(body);
			safeSpansToBuffer(spans);

		} else if (inputType.equals(InputType.V1_THRIFT)) {
			List<zipkin.Span> spans;
			try {
				spans = decodeV1_Thrift(body);
				safeSpansToBuffer(spans);
			} catch (Exception e) {
				e.printStackTrace();
			}


		} else if (inputType.equals(InputType.V2)) {
			List<zipkin2.Span> spans = decodeV2(body);
			safeSpansToBuffer(spans);
		}



		return SUCCESS;
	}

	/**
	 * Converts zipkin spans to {@link AbstractSpan}.
	 */
	private boolean safeSpansToBuffer(List spans) {
		for (Object span : spans) {
			AbstractSpan abstractSpan = ZipkinSpanTransformer.transformSpan(span);
			if (null != abstractSpan) {
			abstractSpan.setPlatformIdent(-1);
			abstractSpan.setMethodIdent(0);
			abstractSpan.setSensorTypeIdent(0);
			idGenerator.assignObjectAnId(abstractSpan);
			buffer.put(new BufferElement<DefaultData>(abstractSpan));
			} else {
				return false;
			}
		}
		return true;

	}

	private static final ThreadLocal<byte[]> GZIP_BUFFER = new ThreadLocal<byte[]>() {
		@Override
		protected byte[] initialValue() {
			return new byte[1024];
		}
	};

	/**
	 * Uses
	 * 
	 * @param input
	 * @return
	 * @throws IOException
	 */
	static byte[] gunzip(byte[] input) throws IOException {
		GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(input));
		try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream(input.length)) {
			byte[] buf = GZIP_BUFFER.get();
			int len;
			while ((len = in.read(buf)) > 0) {
				outputStream.write(buf, 0, len);
			}
			return outputStream.toByteArray();
		}
	}

	/**
	 * V1 decoder
	 * 
	 * @param span
	 * @return
	 */
	private List<zipkin.Span> decodeV1(byte[] span) {
		SpanDecoder decoder = SpanDecoder.JSON_DECODER;
		return decoder.readSpans(span);
	}

	/**
	 * V2 decoder
	 * 
	 * @param span
	 * @return
	 */
	private List<zipkin.Span> decodeV1_Thrift(byte[] span) {
		SpanDecoder decoder = SpanDecoder.THRIFT_DECODER;
		return decoder.readSpans(span);
	}

	private List<zipkin2.Span> decodeV2(byte[] span) {
		List<zipkin2.Span> result = new ArrayList<>();
		if (!SpanBytesDecoder.JSON_V2.decodeList(span, result)) {
			return Collections.emptyList();
		} else {
			return result;
		}
	}

	/**
	 * Handling of all the exceptions happening in this controller.
	 *
	 * @param exception
	 *            Exception being thrown
	 * @return {@link ModelAndView}
	 */
	@ExceptionHandler(Exception.class)
	public ModelAndView handleAllException(Exception exception) {
		return new JsonError(exception).asModelAndView();
	}

	/**
	 * Header information for swagger requests.
	 *
	 * @param response
	 *            Response information
	 */
	@ModelAttribute
	public void setVaryResponseHeader(HttpServletResponse response) {
		response.setHeader("Access-Control-Allow-Origin", "*");
		response.setHeader("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept");
	}

}