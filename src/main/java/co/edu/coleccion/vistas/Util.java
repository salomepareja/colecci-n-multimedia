package co.edu.coleccion.vistas;

import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;

import co.edu.coleccion.modelo.Elemento;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Pequeñas ayudas que usan varias vistas (para no repetir código).
 */
public final class Util {

    public static final Locale LOCALE_ES = Locale.of("es", "CO");
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private Util() {
    }

    /** Fecha como dd/MM/yyyy, o un guion si no hay fecha. */
    public static String fecha(LocalDate fecha) {
        return fecha == null ? "—" : FORMATO_FECHA.format(fecha);
    }

    /** Ícono propio de cada tipo de elemento. */
    public static Icon icono(String tipo) {
        return switch (tipo) {
            case Elemento.PELICULA -> VaadinIcon.FILM.create();
            case Elemento.SERIE -> VaadinIcon.FILM.create();
            case Elemento.MUSICA -> VaadinIcon.MUSIC.create();
            default -> VaadinIcon.FOLDER.create();
        };
    }

    /** Calificación como texto, por ejemplo ★★★☆☆ (sirve para ordenar y para la parte inicial). */
    public static String estrellasTexto(int calificacion) {
        if (calificacion <= 0) {
            return "Sin calificar";
        }
        return "★".repeat(calificacion) + "☆".repeat(5 - calificacion);
    }
}