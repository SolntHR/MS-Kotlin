package cl.induztek.protocolo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RecepcionRequest(
        @NotBlank(message = "Ingresa el nombre de quien recibe")
        @Size(max = 100, message = "El nombre admite hasta 100 caracteres")
        String receptorNombre,

        @NotBlank(message = "Ingresa el RUT de quien recibe")
        @Pattern(regexp = "^\\d{1,2}\\.?\\d{3}\\.?\\d{3}-[\\dkK]$", message = "El RUT debe tener el formato 12.345.678-9")
        String receptorRut,

        @NotBlank(message = "Ingresa el cargo de quien recibe")
        @Size(max = 80, message = "El cargo admite hasta 80 caracteres")
        String receptorCargo) {
}
