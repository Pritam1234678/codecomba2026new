package com.example.codecombat2026.proctoring.event;

import com.example.codecombat2026.proctoring.entity.RiskBand;

/**
 * Fired by {@code ProctoringEventService.ingest} (task 5.3) whenever the
 * sliding {@code risk_score} crosses a band boundary (Req 12.5). Consumed
 * by {@link com.example.codecombat2026.proctoring.event.ProctoringSseListener}
 * which fans the change out to every admin SSE subscriber for the
 * matching contest.
 *
 * <p>This event is intentionally an in-process Spring
 * {@code ApplicationEvent} — there is no Pub/Sub or out-of-process
 * delivery here. Multi-instance fan-out (the 1k+ scaling tier) is
 * documented in the design as a future Valkey Pub/Sub bridge that would
 * republish this same payload onto a channel.
 *
 * <p>{@code newScore} is the post-change cumulative score; {@code oldBand}
 * may equal {@code newBand} only on a rescore replay, but in normal flow
 * the listener may assume the band actually changed.
 */
public record ProctoringRiskBandChangedEvent(
        Long sessionId,
        Long contestId,
        int newScore,
        RiskBand oldBand,
        RiskBand newBand
) {}
