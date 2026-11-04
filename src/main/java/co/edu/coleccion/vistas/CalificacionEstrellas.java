package co.edu.coleccion.vistas;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

/**
 * Calificación de 1 a 5 estrellas hecha con 5 íconos clickeables.
 * Es un campo propio (CustomField<Integer>), así que se usa igual que un TextField:
 * setValue / getValue / addValueChangeListener. Se reutiliza en el formulario, en el Grid
 * y (en modo solo lectura) en las fichas y las estadísticas.
 *
 * Valor 0 = sin calificar. Tocar la estrella que ya está marcada quita la calificación.
 */
public class CalificacionEstrellas extends CustomField<Integer> {

    private HorizontalLayout caja;
    private int valor;

    public CalificacionEstrellas() {
        super(0);
        caja = new HorizontalLayout();
        caja.setPadding(false);
        caja.setSpacing(false);
        add(caja);
        pintar();
    }

    /** Dibuja las 5 estrellas: llenas hasta el valor actual y vacías después. */
    private void pintar() {
        if (caja == null) { // la clase padre puede llamar a este método antes de que exista la caja
            return;
        }
        caja.removeAll();
        for (int posicion = 1; posicion <= 5; posicion++) {
            caja.add(crearEstrella(posicion));
        }
    }

    private Icon crearEstrella(int posicion) {
        boolean llena = posicion <= valor;
        Icon estrella = (llena ? VaadinIcon.STAR : VaadinIcon.STAR_O).create();
        estrella.setSize("1.5rem");
        estrella.setColor(llena ? "#f5b301" : "#9aa0a6");
        estrella.getStyle().set("padding", "0.15rem");
        estrella.getStyle().set("cursor", isReadOnly() ? "default" : "pointer");
        estrella.addClickListener(evento -> seleccionar(posicion));
        return estrella;
    }

    private void seleccionar(int posicion) {
        if (isReadOnly()) {
            return;
        }
        setValue(posicion == valor ? 0 : posicion);
    }

    @Override
    protected Integer generateModelValue() {
        return valor;
    }

    @Override
    protected void setPresentationValue(Integer nuevoValor) {
        valor = nuevoValor == null ? 0 : nuevoValor;
        pintar();
    }

    /** Al mostrarse en pantalla se vuelven a dibujar las estrellas (así toman el modo de solo lectura). */
    @Override
    protected void onAttach(AttachEvent evento) {
        super.onAttach(evento);
        pintar();
    }
}