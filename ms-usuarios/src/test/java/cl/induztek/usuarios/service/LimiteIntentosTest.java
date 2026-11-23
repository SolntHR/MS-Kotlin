package cl.induztek.usuarios.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import cl.induztek.usuarios.exception.DemasiadosIntentosException;

class LimiteIntentosTest {

    private static class RelojMovible extends Clock {

        private Instant ahora = Instant.parse("2026-11-24T12:00:00Z");

        void avanzarMinutos(long minutos) {
            ahora = ahora.plusSeconds(minutos * 60);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return ahora;
        }
    }

    private final RelojMovible reloj = new RelojMovible();
    private final LimiteIntentos limite = new LimiteIntentos(reloj);

    @Test
    void bloqueaAlSuperarElMaximoConElMensajeDeEspera() {
        for (int i = 0; i < 3; i++) {
            limite.verificar("login", "a@b.cl", 3);
        }

        assertThatThrownBy(() -> limite.verificar("login", " A@B.cl ", 3))
                .isInstanceOf(DemasiadosIntentosException.class).hasMessage(LimiteIntentos.MENSAJE);
    }

    @Test
    void cadaAccionEIdentidadSeCuentaPorSeparado() {
        for (int i = 0; i < 3; i++) {
            limite.verificar("login", "a@b.cl", 3);
        }

        assertThatCode(() -> limite.verificar("codigo", "a@b.cl", 3)).doesNotThrowAnyException();
        assertThatCode(() -> limite.verificar("login", "otro@b.cl", 3)).doesNotThrowAnyException();
        assertThatCode(() -> limite.verificar("login", null, 3)).doesNotThrowAnyException();
    }

    @Test
    void seLiberaAlPasarLos15MinutosOAlReiniciar() {
        for (int i = 0; i < 3; i++) {
            limite.verificar("login", "a@b.cl", 3);
            limite.verificar("registro", "a@b.cl", 3);
        }

        reloj.avanzarMinutos(15);
        assertThatCode(() -> limite.verificar("login", "a@b.cl", 3)).doesNotThrowAnyException();

        limite.reiniciar("registro", "a@b.cl");
        assertThatCode(() -> limite.verificar("registro", "a@b.cl", 3)).doesNotThrowAnyException();
    }
}
