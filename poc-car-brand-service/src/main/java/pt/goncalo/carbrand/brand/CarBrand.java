package pt.goncalo.carbrand.brand;

import jakarta.validation.constraints.NotBlank;

/**
 * Car brand, used as input and output by REST and MCP. The name is the unique key (case-insensitive).
 *
 * @param name brand name, e.g. "Toyota"
 * @param country country of origin, e.g. "Japan"
 */
public record CarBrand(@NotBlank String name, @NotBlank String country) {
}
