package rocks.inspectit.server.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import org.spec.research.open.xtrace.adapters.inspectit.source.InspectITTraceConverter;
import org.spec.research.open.xtrace.api.core.Trace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import rocks.inspectit.server.cache.IBuffer;
import rocks.inspectit.server.dao.impl.PlatformIdentDaoImpl;
import rocks.inspectit.shared.all.cmr.model.PlatformIdent;
import rocks.inspectit.shared.all.communication.DefaultData;
import rocks.inspectit.shared.all.communication.data.InvocationSequenceData;
import rocks.inspectit.shared.all.tracing.data.AbstractSpan;
import rocks.inspectit.shared.all.tracing.data.Span;
import rocks.inspectit.shared.cs.cmr.service.IInvocationDataAccessService;
import rocks.inspectit.shared.cs.cmr.service.ISpanService;
import rocks.inspectit.shared.cs.communication.comparator.ResultComparator;

@Service
public class OPENxtraceAccessService {
	@Autowired
	private PlatformIdentDaoImpl platformIdentDao;

	@Autowired
	private IBuffer<DefaultData> buffer;

	@Autowired
	private IInvocationDataAccessService dataAccessService;

	@Autowired
	private ISpanService spansService;

	public List<Trace> getOpenXTRACETraces(int limit, Date fromDate, Date toDate, ResultComparator<AbstractSpan> resultComparator) {
		List<Trace> resultList = new ArrayList<Trace>();
		// Get all PlatformIdents
		List<PlatformIdent> platformIdentList = platformIdentDao.findAll();

		List<InvocationSequenceData> listSequences = dataAccessService.getInvocationSequenceOverview(0, -1, null);
		List<InvocationSequenceData> listSequencesDetail = new LinkedList<InvocationSequenceData>();

		// Get all invocs
		for (InvocationSequenceData invocationSequenceData : listSequences) {
			InvocationSequenceData invocDetail = dataAccessService.getInvocationSequenceDetail(invocationSequenceData);
			// is already nested sequence?
			if (invocDetail != null) {
				listSequencesDetail.add(invocDetail);
			}
		}

		InspectITTraceConverter converter = new InspectITTraceConverter();

		Collection<? extends Span> rootSpans = spansService.getRootSpans(limit, fromDate, toDate, resultComparator);

		// convert spans and trigger diagnoseIT
		for (Span span : rootSpans) {
			HashSet<Span> abstractSpans = new HashSet<Span>();
			abstractSpans.addAll(spansService.getSpans(span.getSpanIdent().getTraceId()));
			abstractSpans.add(span);
			resultList.add(converter.convertTraces(listSequencesDetail, platformIdentList, new ArrayList<Span>(abstractSpans)));
		}
		return resultList;
	}

}
