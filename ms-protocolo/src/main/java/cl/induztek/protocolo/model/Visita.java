package cl.induztek.protocolo.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "visita")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Visita {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_ot", nullable = false, unique = true, length = 20)
    private String numeroOt;

    @Column(name = "instalacion_id", nullable = false)
    private Long instalacionId;

    @Column(nullable = false)
    private LocalDate fecha;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_visita", nullable = false, length = 20)
    private TipoVisita tipoVisita;

    @Column(name = "ingeniero_cargo_id", nullable = false)
    private Long ingenieroCargoId;

    @ElementCollection
    @CollectionTable(name = "visita_participante", joinColumns = @JoinColumn(name = "visita_id"))
    @Column(name = "usuario_id", nullable = false)
    @Builder.Default
    private Set<Long> participantes = new LinkedHashSet<>();

    @Column(name = "fecha_hora_cierre")
    private LocalDateTime fechaHoraCierre;

    @Embedded
    private RecepcionActa recepcion;

    public boolean estaCerrada() {
        return fechaHoraCierre != null;
    }

    public boolean participa(Long usuarioId) {
        return ingenieroCargoId.equals(usuarioId) || participantes.contains(usuarioId);
    }
}
