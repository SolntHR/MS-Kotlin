package cl.induztek.protocolo.model;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "prueba")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Prueba {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "protocolo_id", nullable = false)
    private Protocolo protocolo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tipo_prueba_id", nullable = false)
    private TipoPrueba tipoPrueba;

    @Column(nullable = false, length = 40)
    private String elemento;

    @Column(length = 20)
    private String momento;

    @Column(nullable = false)
    private boolean realizada;

    @Column(name = "motivo_no_realizada", length = 255)
    private String motivoNoRealizada;

    @Column(name = "instrumento_id")
    private Long instrumentoId;

    @Enumerated(EnumType.STRING)
    @Column(length = 15)
    private Resultado resultado;

    @Column(length = 255)
    private String comentario;

    @Column(name = "ingresado_manualmente", nullable = false)
    private boolean ingresadoManualmente;

    @OneToMany(mappedBy = "prueba", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden")
    @Builder.Default
    private List<Medicion> mediciones = new ArrayList<>();

    public void agregarMedicion(Medicion medicion) {
        medicion.setPrueba(this);
        mediciones.add(medicion);
    }
}
