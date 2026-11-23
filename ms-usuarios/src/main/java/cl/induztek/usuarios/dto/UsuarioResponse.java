package cl.induztek.usuarios.dto;

import cl.induztek.usuarios.model.Usuario;

public record UsuarioResponse(
        Long id,
        String rolCodigo,
        String rolNombre,
        String rut,
        String nombre,
        String correo,
        String correoPersonal,
        String telefono,
        boolean activo,
        boolean tieneContrasena,
        String giro,
        String contactoNombre,
        String contactoCargo) {

    public static UsuarioResponse desde(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getRol().name(),
                usuario.getRol().getNombre(),
                usuario.getRut(),
                usuario.getNombre(),
                usuario.getCorreo(),
                usuario.getCorreoPersonal(),
                usuario.getTelefono(),
                usuario.isActivo(),
                usuario.tieneContrasena(),
                usuario.getGiro(),
                usuario.getContactoNombre(),
                usuario.getContactoCargo());
    }
}
