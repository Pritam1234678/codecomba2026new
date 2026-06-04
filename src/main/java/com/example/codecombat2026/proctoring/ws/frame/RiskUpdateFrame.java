package com.example.codecombat2026.proctoring.ws.frame;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Outbound {@code RISK_UPDATE} frame (Req 10.1, 12.5).
 *
 * <p>Wire shape:
 * <pre>{@code
 * { "type": "RISK_UPDATE", "risk_score": 75, "risk_band": "MEDIUM" }
 * }</pre>
 *
 * <p>Sent as the very first server frame after handshake (with the current
 * Valkey state, default {@code 0}/{@code LOW} for a brand-new session) and
 * thereafter on every band crossing.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RiskUpdateFrame(
        @JsonProperty("type") String type,
        @JsonProperty("risk_score") int riskScore,
        @JsonProperty("risk_band") String riskBand) {

    public static RiskUpdateFrame of(int riskScore, String riskBand) {
        return new RiskUpdateFrame("RISK_UPDATE", riskScore, riskBand);
    }
}
