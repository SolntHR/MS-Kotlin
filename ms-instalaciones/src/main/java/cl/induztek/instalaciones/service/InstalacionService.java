package cl.induztek.instalaciones.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.induztek.instalaciones.client.UsuarioClient;
import cl.induztek.instalaciones.dto.ClienteResumen;
import cl.induztek.instalaciones.dto.InstalacionRequest;
import cl.induztek.instalaciones.dto.InstalacionResponse;
import cl.induztek.instalaciones.exception.ConflictoException;
import cl.induztek.instalaciones.exception.RecursoNoEncontradoException;
import cl.induztek.instalaciones.exception.SolicitudInvalidaException;
import cl.induztek.instalaciones.model.Comuna;
import cl.induztek.instalaciones.model.Instalacion;
import cl.induztek.instalaciones.repository.ComunaRepository;
import cl.induztek.instalaciones.repository.InstalacionRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InstalacionService {

    private final InstalacionRepository instalacionRepository;
    private final ComunaRepository comunaRepository;
    private final UsuarioClient usuarioClient;

    @Transactional(readOnly = true)
    public List<InstalacionResponse> listar(Long clienteId) {
        List<Instalacion> instalaciones = clienteId == null
                ? instalacionRepository.findAllByOrderByNombreAsc()
                : instalacionRepository.findByClienteIdOrderByNombreAsc(clienteId);
        Map<Long, String> nombres = usuarioClient.nombresClientes();
        return instalaciones.stream()
                .map(instalacion -> InstalacionResponse.desde(instalacion, nombres.get(instalacion.getClienteId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public InstalacionResponse obtener(Long id) {
        Instalacion instalacion = buscar(id);
        return InstalacionResponse.desde(instalacion, usuarioClient.nombreCliente(instalacion.getClienteId()));
    }

    @Transactional
    public InstalacionResponse crear(InstalacionRequest request) {
        ClienteResumen cliente = validarCliente(request.clienteId());
        String nombre = request.nombre().trim();
        if (instalacionRepository.existsByClienteIdAndNombreIgnoreCase(cliente.id(), nombre)) {
            throw new ConflictoException("El cliente ya tiene una instalación llamada " + nombre);
        }
        Instalacion instalacion = Instalacion.builder()
                .clienteId(cliente.id())
                .nombre(nombre)
                .comuna(buscarComuna(request.comunaId()))
                .ubicacion(limpiar(request.ubicacion()))
                .observacionesAcceso(limpiar(request.observacionesAcceso()))
                .build();
        return InstalacionResponse.desde(instalacionRepository.save(instalacion), cliente.nombre());
    }

    @Transactional
    public InstalacionResponse actualizar(Long id, InstalacionRequest request) {
        Instalacion instalacion = buscar(id);
        ClienteResumen cliente = validarCliente(request.clienteId());
        String nombre = request.nombre().trim();
        if (instalacionRepository.existsByClienteIdAndNombreIgnoreCaseAndIdNot(cliente.id(), nombre, id)) {
            throw new ConflictoException("El cliente ya tiene una instalación llamada " + nombre);
        }
        instalacion.setClienteId(cliente.id());
        instalacion.setNombre(nombre);
        instalacion.setComuna(buscarComuna(request.comunaId()));
        instalacion.setUbicacion(limpiar(request.ubicacion()));
        instalacion.setObservacionesAcceso(limpiar(request.observacionesAcceso()));
        return InstalacionResponse.desde(instalacionRepository.save(instalacion), cliente.nombre());
    }

    Instalacion buscar(Long id) {
        return instalacionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la instalación " + id));
    }

    private ClienteResumen validarCliente(Long clienteId) {
        ClienteResumen cliente = usuarioClient.obtenerCliente(clienteId);
        if (!"CLIENTE".equals(cliente.rolCodigo())) {
            throw new SolicitudInvalidaException("El usuario seleccionado no es un cliente");
        }
        if (!cliente.activo()) {
            throw new SolicitudInvalidaException("La cuenta del cliente está desactivada");
        }
        return cliente;
    }

    private Comuna buscarComuna(Long comunaId) {
        return comunaRepository.findById(comunaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la comuna " + comunaId));
    }

    private String limpiar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
