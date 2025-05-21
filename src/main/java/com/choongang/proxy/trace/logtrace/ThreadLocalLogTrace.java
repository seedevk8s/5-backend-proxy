package com.choongang.proxy.trace.logtrace;

import com.choongang.proxy.trace.TraceId;
import com.choongang.proxy.trace.TraceStatus;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ThreadLocalLogTrace implements LogTrace{

    private static final String START_PREFIX = "-->";
    private static final String COMPLETE_PREFIX = "<--";
    private static final String EX_PREFIX = "<X-";

    //private TraceId traceIdHolder; // TraceId를 보관하는 필드
    private ThreadLocal<TraceId> traceIdHolder = new ThreadLocal<>(); // ThreadLocal을 사용하여 TraceId를 보관

    @Override
    public TraceStatus begin(String message) {
        syncTraceId();
        TraceId traceId = traceIdHolder.get();
        long startTimeMs = System.currentTimeMillis();
        log.info("[{}] {}{}", traceId.getId(), addSpace(START_PREFIX, traceId.getLevel()), message);
        return new TraceStatus(traceId, startTimeMs, message);
    }

    private void syncTraceId() {
        // ThreadLocal에서 TraceId를 가져옴
        TraceId traceId = traceIdHolder.get();
        if (traceId == null) {
            traceIdHolder.set(new TraceId());
        } else {
            traceIdHolder.set(traceId.createNextId()); // TraceId의 레벨을 하나 올림
        }
    }

    @Override
    public void end(TraceStatus status) {
        complete(status, null);
    }

    private void complete(TraceStatus status, Exception e) {
        Long stopTimeMs = System.currentTimeMillis();
        long resultTimeMs = stopTimeMs - status.getStartTimeMs();
        TraceId traceId = status.getTraceId();

        if (e == null) {
            log.info("[{}] {}{} time={}ms", traceId.getId(), addSpace(COMPLETE_PREFIX, traceId.getLevel()), status.getMessage(), resultTimeMs);
        } else {
            log.info("[{}] {}{} time={}ms ex={}", traceId.getId(), addSpace(EX_PREFIX, traceId.getLevel()), status.getMessage(), resultTimeMs, e.toString());
        }

        releaseTraceId();

    }

    private void releaseTraceId() {
        // ThreadLocal에서 TraceId를 가져옴
        TraceId traceId = this.traceIdHolder.get();
        if (traceId.isFirstLevel()) {
            traceIdHolder.remove(); // ThreadLocal에서 TraceId를 제거
        } else {
            traceIdHolder.set(traceId.createPreviousId());
        }
    }

    @Override
    public void exception(TraceStatus status, Exception e) {
        complete(status, e);
    }

    private static String addSpace(String prefix, int level) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < level; i++) {
            sb.append(i == level - 1 ? "|" + prefix : "|   ");
        }
        return sb.toString();
    }
}
