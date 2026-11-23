package cl.induztek.usuarios.model;

public enum Rol {
    TECNICO("Técnico de terreno"),
    INGENIERO("Ingeniero de pruebas"),
    JEFE_TECNICO("Jefe técnico"),
    CLIENTE("Usuario cliente"),
    SIN_ASIGNAR("Sin asignar");

    private final String nombre;

    Rol(String nombre) {
        this.nombre = nombre;
    }

    public String getNombre() {
        return nombre;
    }

    public boolean esPersonalInduztek() {
        return this == TECNICO || this == INGENIERO || this == JEFE_TECNICO;
    }
}
