package cl.induztek.protocolo.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecepcionActa {

    @Column(name = "receptor_nombre", length = 100)
    private String nombre;

    @Column(name = "receptor_rut", length = 12)
    private String rut;

    @Column(name = "receptor_cargo", length = 80)
    private String cargo;

    @Column(name = "fecha_hora_recepcion")
    private LocalDateTime fechaHora;
}
