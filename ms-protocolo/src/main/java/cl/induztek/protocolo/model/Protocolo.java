package cl.induztek.protocolo.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "protocolo", uniqueConstraints = @UniqueConstraint(columnNames = {"visita_id", "equipo_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Protocolo {
    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, unique = true, length = 20)
    private String numero;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "visita_id", nullable = false)
    private Visita visita;

    @Column(name = "equipo_id", nullable = false)
    private Long equipoId;

    @Column(name = "usuario_registra_id", nullable = false)
    private Long usuarioRegistraId;

    @Column(name = "fecha_hora_prueba", nullable = false)
    private LocalDateTime fechaHoraPrueba;

    @Column(name = "temperatura_c")
    private Double temperaturaC;

    @Column(name = "humedad_pct")
    private Double humedadPct;

    @Column(name = "descripcion_mantenimiento", length = 500)
    private String descripcionMantenimiento;

    @Column(length = 500)
    private String observaciones;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoProtocolo estado;

    @Column(name = "protocolo_reemplazado_id", length = 36)
    private String protocoloReemplazadoId;

    @OneToMany(mappedBy = "protocolo", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    @Builder.Default
    private List<Prueba> pruebas = new ArrayList<>();

    @OneToMany(mappedBy = "protocolo", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("fechaHora, id")
    @Builder.Default
    private List<HistorialEstado> historial = new ArrayList<>();

    public void agregarPrueba(Prueba prueba) {
        prueba.setProtocolo(this);
        pruebas.add(prueba);
    }

    public void registrarCambio(EstadoProtocolo nuevo, Long usuarioId, LocalDateTime fechaHora, String motivo) {
        historial.add(HistorialEstado.builder().protocolo(this).estadoAnterior(estado).estadoNuevo(nuevo)
                .usuarioId(usuarioId).fechaHora(fechaHora).motivo(motivo).build());
        estado = nuevo;
    }
}
