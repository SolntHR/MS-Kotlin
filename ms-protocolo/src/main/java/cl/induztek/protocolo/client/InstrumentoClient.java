package cl.induztek.protocolo.client;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import cl.induztek.protocolo.dto.remoto.VigenciaRemota;

@Component
public class InstrumentoClient extends ClienteRemoto {

    public InstrumentoClient(WebClient.Builder builder, @Value("${servicios.instrumentos.url}") String url) {
        super(builder, url, "de instrumentos");
    }

    public VigenciaRemota vigencia(Long instrumentoId, LocalDate fecha) {
        return obtener(cliente -> cliente.get()
                .uri(uri -> uri.path("/instrumentos/{id}/vigencia").queryParam("fecha", fecha).build(instrumentoId))
                .retrieve().bodyToMono(VigenciaRemota.class), "No existe el instrumento " + instrumentoId);
    }
}
