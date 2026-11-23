package pt.goncalo.aigateway.audit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Writes one audit record per MCP call.
 * <p>
 * POC: logs a structured line. Target: synchronous insert into a DB, fail-open with an alert.
 */
@Slf4j
@Component
public class AuditLogger {

	/**
	 * Records an MCP call. Never throws: an audit problem must not block the AI call (fail-open).
	 *
	 * @param entry audit data
	 */
	public void record(AuditEntry entry) {
		try {
			log.info("AUDIT client={} user={} method={} tool={} upstream={} outcome={} durationMs={}",
					entry.clientId(), entry.userId(), entry.method(), entry.tool(), entry.upstream(), entry.outcome(),
					entry.durationMs());
		}
		catch (RuntimeException ex) {
			log.error("Audit write failed for client={} tool={}", entry.clientId(), entry.tool(), ex);
		}
	}

}
