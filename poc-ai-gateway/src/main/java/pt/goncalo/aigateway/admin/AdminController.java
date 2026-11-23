package pt.goncalo.aigateway.admin;

import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pt.goncalo.aigateway.registry.RegisteredTool;
import pt.goncalo.aigateway.registry.ToolRegistry;
import pt.goncalo.aigateway.registry.ToolStatus;

/**
 * Admin API over the registry (the future admin UI talks to this).
 * <p>
 * POC: unauthenticated, in memory. Target: admin role required, every change audited, DB-backed.
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
class AdminController {

	private final ToolRegistry registry;

	/**
	 * @return all clients and whether they are enabled
	 */
	@GetMapping("/clients")
	Map<String, Boolean> clients() {
		return registry.clients();
	}

	/**
	 * Registers, enables or disables an AI client. Disabled clients get {@code 403} on their next request.
	 *
	 * @param clientId AI client id
	 * @param update new state
	 * @return all clients
	 */
	@PutMapping("/clients/{clientId}")
	Map<String, Boolean> updateClient(@PathVariable String clientId, @Valid @RequestBody ClientUpdate update) {
		registry.setClientEnabled(clientId, update.enabled());
		return registry.clients();
	}

	/**
	 * @return all known tools including discovered, not yet approved ones
	 */
	@GetMapping("/tools")
	List<ToolView> tools() {
		return registry.tools().stream().map(ToolView::of).toList();
	}

	/**
	 * Changes status, client grants and description of a tool.
	 *
	 * @param upstream upstream id
	 * @param internalName tool name on the upstream
	 * @param update new rules
	 * @return updated tool, {@code 404} if unknown
	 */
	@PutMapping("/tools/{upstream}/{internalName}")
	ResponseEntity<ToolView> updateTool(@PathVariable String upstream, @PathVariable String internalName,
			@Valid @RequestBody ToolUpdate update) {
		return registry
			.updateTool(upstream + "/" + internalName, update.status(),
					update.clients() != null ? update.clients() : List.of(), update.description())
			.map(ToolView::of)
			.map(ResponseEntity::ok)
			.orElse(ResponseEntity.notFound().build());
	}

	/**
	 * @param enabled new client state
	 */
	record ClientUpdate(@NotNull Boolean enabled) {
	}

	/**
	 * @param status new status
	 * @param clients granted clients, empty or {@code null} = every enabled client
	 * @param description description override, {@code null} = upstream description
	 */
	record ToolUpdate(@NotNull ToolStatus status, List<String> clients, String description) {
	}

	/**
	 * Admin view of a tool.
	 */
	record ToolView(String id, String publicName, ToolStatus status, List<String> clients, String description,
			Boolean readOnly, Boolean destructive) {

		static ToolView of(RegisteredTool tool) {
			var definition = tool.publicDefinition();
			var annotations = definition.annotations();
			return new ToolView(tool.id(), tool.publicName(), tool.status(), tool.clients(), definition.description(),
					annotations != null ? annotations.readOnlyHint() : null,
					annotations != null ? annotations.destructiveHint() : null);
		}

	}

}
