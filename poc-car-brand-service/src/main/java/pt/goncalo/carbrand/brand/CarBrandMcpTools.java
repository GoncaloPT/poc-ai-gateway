package pt.goncalo.carbrand.brand;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpTool.McpAnnotations;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

/**
 * MCP adapter for car brands. Thin layer next to the REST controller: it only maps tool parameters and delegates to
 * {@link CarBrandService}. Tools are discovered automatically by the Spring AI MCP annotation scanner.
 * <p>
 * These tools are internal. Whether an external AI client can see them is decided by the AI gateway.
 */
@Component
@RequiredArgsConstructor
class CarBrandMcpTools {

	private final CarBrandService carBrandService;

	/**
	 * Adds a car brand to the list.
	 */
	@McpTool(name = "add_car_brand",
			description = "Add a car brand to the list. Fails if a brand with the same name already exists.",
			annotations = @McpAnnotations(readOnlyHint = false, destructiveHint = false, idempotentHint = false))
	public CarBrand addCarBrand(@McpToolParam(description = "Brand name, e.g. BMW") String name,
			@McpToolParam(description = "Country of origin, e.g. Germany") String country) {
		return carBrandService.addBrand(new CarBrand(name, country));
	}

	/**
	 * Lists all car brands.
	 */
	@McpTool(name = "list_car_brands", description = "List all car brands with name and country, ordered by name.",
			annotations = @McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true,
					openWorldHint = false))
	public List<CarBrand> listCarBrands() {
		return carBrandService.listBrands();
	}

	/**
	 * Removes a car brand. Not approved in the AI gateway registry up front (shows up there as DISCOVERED).
	 */
	@McpTool(name = "remove_car_brand", description = "Remove a car brand by name. Returns the removed brand.",
			annotations = @McpAnnotations(readOnlyHint = false, destructiveHint = true, idempotentHint = true))
	public CarBrand removeCarBrand(@McpToolParam(description = "Brand name, case-insensitive") String name) {
		return carBrandService.removeBrand(name);
	}

}
