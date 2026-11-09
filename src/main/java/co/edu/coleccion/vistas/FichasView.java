package co.edu.coleccion.vistas;

import co.edu.coleccion.datos.Almacen;
import co.edu.coleccion.modelo.Elemento;
import co.edu.coleccion.modelo.Lista;
import co.edu.coleccion.modelo.Prestamo;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Fichas desplegables: cada elemento es un componente Details que, al abrirlo,
 * muestra todos sus datos, su estado de préstamo, su historial y las listas donde está.
 * Funciona bien en el celular: todo va en una sola columna.
 */
@Route(value = "fichas", layout = MainLayout.class)
@PageTitle("Fichas")
public class FichasView extends VerticalLayout {

    private final Almacen almacen = Almacen.get();

    private final TextField buscar = new TextField();
    private final VerticalLayout contenedor = new VerticalLayout();

    public FichasView() {
        setMaxWidth("50rem");

        buscar.setPlaceholder("Buscar por título o creador");
        buscar.setPrefixComponent(VaadinIcon.SEARCH.create());
        buscar.setClearButtonVisible(true);
        buscar.setValueChangeMode(ValueChangeMode.EAGER);
        buscar.setWidthFull();
        buscar.addValueChangeListener(e -> mostrarFichas());

        contenedor.setPadding(false);
        contenedor.setSpacing(false);

        add(new H2("Fichas"), buscar, contenedor);
        mostrarFichas();
    }

    private void mostrarFichas() {
        String texto = buscar.getValue().trim().toLowerCase();
        List<Elemento> lista = almacen.getElementos().stream()
                .filter(e -> texto.isEmpty()
                        || e.getTitulo().toLowerCase().contains(texto)
                        || e.getCreador().toLowerCase().contains(texto))
                .toList();
        contenedor.removeAll();
        if (lista.isEmpty()) {
            contenedor.add(new Span("No hay elementos que coincidan con la búsqueda."));
            return;
        }
        for (Elemento elemento : lista) {
            contenedor.add(crearFicha(elemento));
        }
    }

    /** Una ficha: el resumen (siempre visible) y el contenido (se despliega al tocarla). */
    private Details crearFicha(Elemento e) {
        Span nombre = new Span(e.getTitulo() + " (" + e.getAnio() + ")");
        nombre.getStyle().set("font-weight", "600");
        HorizontalLayout resumen = new HorizontalLayout(Util.icono(e.getTipo()), nombre);
        resumen.setAlignItems(FlexComponent.Alignment.CENTER);
        if (almacen.estaPrestado(e.getId())) {
            Span marca = new Span("· prestado");
            marca.getStyle().set("color", "var(--lumo-error-text-color, #c62828)");
            resumen.add(marca);
        }

        VerticalLayout contenido = new VerticalLayout();
        contenido.setPadding(false);
        contenido.setSpacing(false);
        contenido.add(
                dato("Tipo", e.getTipo()),
                dato("Género", e.getGenero()),
                dato("Año", String.valueOf(e.getAnio())),
                dato("Director o artista", e.getCreador()));

        CalificacionEstrellas estrellas = new CalificacionEstrellas();
        estrellas.setValue(e.getCalificacion());
        estrellas.setReadOnly(true); // aquí solo se muestra; se califica en la colección
        HorizontalLayout filaCalificacion = new HorizontalLayout(etiqueta("Calificación"), estrellas);
        filaCalificacion.setAlignItems(FlexComponent.Alignment.CENTER);
        contenido.add(filaCalificacion);

        contenido.add(dato("Estado", almacen.prestamoActivo(e.getId())
                .map(p -> "Prestado a " + p.getAmigo() + " desde el " + Util.fecha(p.getFechaPrestamo()))
                .orElse("Disponible")));

        String listas = almacen.getListas().stream()
                .filter(l -> l.getElementoIds().contains(e.getId()))
                .map(Lista::getNombre)
                .collect(Collectors.joining(", "));
        contenido.add(dato("Listas", listas.isEmpty() ? "No está en ninguna lista" : listas));

        contenido.add(crearHistorial(e));

        Details ficha = new Details(resumen, contenido);
        ficha.setWidthFull();
        ficha.getStyle().set("border-bottom", "1px solid var(--lumo-contrast-20pct, #d0d0d0)");
        return ficha;
    }

    private Component crearHistorial(Elemento e) {
        VerticalLayout historial = new VerticalLayout();
        historial.setPadding(false);
        historial.setSpacing(false);
        historial.getStyle().set("margin-top", "0.5rem");
        historial.add(etiqueta("Historial de préstamos"));

        List<Prestamo> prestamos = almacen.prestamosDe(e.getId());
        if (prestamos.isEmpty()) {
            historial.add(new Span("Nunca se ha prestado."));
        }
        for (Prestamo p : prestamos) {
            String texto = p.getAmigo() + " — prestado el " + Util.fecha(p.getFechaPrestamo())
                    + (p.isActivo() ? " (aún sin devolver)" : ", devuelto el " + Util.fecha(p.getFechaDevolucion()));
            historial.add(new Span("• " + texto));
        }
        return historial;
    }

    private Span etiqueta(String texto) {
        Span etiqueta = new Span(texto + ":");
        etiqueta.getStyle().set("font-weight", "600");
        return etiqueta;
    }

    private Component dato(String nombre, String valor) {
        Div fila = new Div();
        Span valorTexto = new Span(" " + valor);
        fila.add(etiqueta(nombre), valorTexto);
        return fila;
    }
}
