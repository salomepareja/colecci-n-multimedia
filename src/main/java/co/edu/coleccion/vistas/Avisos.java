package co.edu.coleccion.vistas;

import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.notification.Notification;

/**
 * Mensajes al usuario: notificaciones de éxito o error y el diálogo de confirmación
 * que usa toda eliminación.
 */
public final class Avisos {

    private Avisos() {
    }

    public static void exito(String mensaje) {
        Notification n = Notification.show(mensaje, 3000, Notification.Position.BOTTOM_START);
        n.addThemeName("success");
    }

    public static void error(String mensaje) {
        Notification n = Notification.show(mensaje, 5000, Notification.Position.MIDDLE);
        n.addThemeName("error");
    }

    /** Muestra un diálogo de confirmación; la acción solo se ejecuta si la persona confirma. */
    public static void confirmar(String titulo, String texto, String textoConfirmar, Runnable accion) {
        ConfirmDialog dialogo = new ConfirmDialog();
        dialogo.setHeader(titulo);
        dialogo.setText(texto);
        dialogo.setCancelable(true);
        dialogo.setCancelText("Cancelar");
        dialogo.setConfirmText(textoConfirmar);
        dialogo.setConfirmButtonTheme("error primary");
        dialogo.addConfirmListener(evento -> accion.run());
        dialogo.open();
    }
}
