package rocks.inspectit.server.service.rest;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

import java.io.IOException;
import java.util.Date;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import rocks.inspectit.server.open.xtrace.OPENxtraceSerializer;
import rocks.inspectit.server.service.OPENxtraceAccessService;

@Controller
@RequestMapping(value = "open-xtrace")
public class OPENxtraceRestfulService {

	@Autowired
	private OPENxtraceAccessService openxtraceAccessService;

	private OPENxtraceSerializer serializer;

	/**
	 * Restful endpoint to get traces, which are in the buffer as OPEN.xtrace.
	 * 
	 * @param fromDate
	 *            earliest date
	 * @param toDate
	 *            latest date
	 * @return JSON formatted openXTRACE
	 * @throws JsonGenerationException
	 * @throws JsonMappingException
	 * @throws IOException
	 */
	@RequestMapping(value = "/get", method = GET)
	@ResponseBody
	public String getOpenXTRACETraces(@RequestParam(value = "fromDate", required = false) @DateTimeFormat(iso = ISO.DATE_TIME) Date fromDate,
			@RequestParam(value = "toDate", required = false) @DateTimeFormat(iso = ISO.DATE_TIME) Date toDate) throws JsonGenerationException, JsonMappingException, IOException {
		return serializer.serialize(openxtraceAccessService.getOpenXTRACETraces(-1, fromDate, toDate, null));
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

	@PostConstruct
	private void initialize() {
		serializer = new OPENxtraceSerializer();
	}

}
