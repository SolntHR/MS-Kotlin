package cl.induztek.usuarios.validacion;

public final class ValidadorRut {

    private ValidadorRut() {
    }

    public static String limpiar(String rut) {
        return rut == null ? "" : rut.replaceAll("[.\\-\\s]", "").toUpperCase();
    }

    public static boolean esValido(String rut) {
        String limpio = limpiar(rut);
        if (!limpio.matches("^\\d{7,8}[\\dK]$")) {
            return false;
        }
        String cuerpo = limpio.substring(0, limpio.length() - 1);
        char digitoIngresado = limpio.charAt(limpio.length() - 1);
        int suma = 0;
        int multiplicador = 2;
        for (int i = cuerpo.length() - 1; i >= 0; i--) {
            suma += Character.getNumericValue(cuerpo.charAt(i)) * multiplicador;
            multiplicador = multiplicador == 7 ? 2 : multiplicador + 1;
        }
        int resto = 11 - (suma % 11);
        char digitoEsperado = resto == 11 ? '0' : resto == 10 ? 'K' : Character.forDigit(resto, 10);
        return digitoEsperado == digitoIngresado;
    }

    public static String formatear(String rut) {
        String limpio = limpiar(rut);
        if (limpio.length() < 2) {
            return limpio;
        }
        String cuerpo = limpio.substring(0, limpio.length() - 1);
        StringBuilder conPuntos = new StringBuilder();
        int contador = 0;
        for (int i = cuerpo.length() - 1; i >= 0; i--) {
            conPuntos.insert(0, cuerpo.charAt(i));
            contador++;
            if (contador % 3 == 0 && i > 0) {
                conPuntos.insert(0, '.');
            }
        }
        return conPuntos + "-" + limpio.charAt(limpio.length() - 1);
    }
}
