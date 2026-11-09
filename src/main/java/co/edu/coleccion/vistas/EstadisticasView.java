package co.edu.coleccion.vistas;

import co.edu.coleccion.datos.Almacen;
import co.edu.coleccion.modelo.Elemento;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Estadísticas de la colección: cantidad por tipo y por género, calificación promedio
 * y los elementos mejor calificados. Las barras están hechas con Div (sin librerías de gráficos).
 */
@Route(value = "estadisticas", layout = MainLayout.class)
@PageTitle("Estadísticas")
public class EstadisticasView extends VerticalLayout {

    private final Almacen almacen = Almacen.get();

    public EstadisticasView() {
        setMaxWidth("60rem");

        List<Elemento> elementos = almacen.getElementos();
        add(new H2("Estadísticas"));

        if (elementos.isEmpty()) {
            add(new Span("Todavía no hay elementos en tu colección."));
            return;
        }

        add(crearResumen(elementos));
        add(new H3("Cantidad por tipo"), crearPorTipo(elementos));
        add(new H3("Cantidad por género"), crearPorGenero(elementos));
        add(new H3("Mejor calificados"), crearTopCalificados(elementos));
    }

    // ------------------------------------------------------------------

    private Component crearResumen(List<Elemento> elementos) {
        long prestados = elementos.stream().filter(e -> almacen.estaPrestado(e.getId())).count();
        long sinCalificar = elementos.stream().filter(e -> e.getCalificacion() == 0).count();

        FlexLayout tarjetas = new FlexLayout(
                tarjeta("Elementos", String.valueOf(elementos.size())),
                tarjeta("Prestados ahora", String.valueOf(prestados)),
                tarjeta("Calificación promedio", promedioTexto(elementos)),
                tarjeta("Sin calificar", String.valueOf(sinCalificar)));
        tarjetas.setFlexWrap(FlexLayout.FlexWrap.WRAP);
        tarjetas.getStyle().set("gap", "0.75rem");
        return tarjetas;
    }

    private Component tarjeta(String titulo, String valor) {
        Span numero = new Span(valor);
        numero.getStyle().set("font-size", "1.8rem").set("font-weight", "700");
        Span etiqueta = new Span(titulo);
        etiqueta.getStyle().set("font-size", "0.85rem").set("opacity", "0.75");

        Div caja = new Div(numero, etiqueta);
        caja.getStyle().set("display", "flex").set("flex-direction", "column")
                .set("padding", "0.75rem 1rem")
                .set("border-radius", "0.5rem")
                .set("background", "var(--lumo-contrast-5pct, #f1f1f1)")
                .set("min-width", "9rem").set("flex", "1 1 9rem");
        return caja;
    }

    /** Promedio de las calificaciones (sin contar los elementos sin calificar), con una cifra decimal. */
    private String promedioTexto(List<Elemento> elementos) {
        return elementos.stream()
                .filter(e -> e.getCalificacion() > 0)
                .mapToInt(Elemento::getCalificacion)
                .average()
                .stream()
                .mapToObj(promedio -> String.format(Util.LOCALE_ES, "%.1f ★", promedio))
                .findFirst()
                .orElse("—");
    }

    private Component crearPorTipo(List<Elemento> elementos) {
        VerticalLayout filas = nuevoContenedor();
        long maximo = 0;
        for (String tipo : Elemento.TIPOS) {
            maximo = Math.max(maximo, cantidadDeTipo(elementos, tipo));
        }
        for (String tipo : Elemento.TIPOS) {
            List<Elemento> deEsteTipo = elementos.stream().filter(e -> e.getTipo().equals(tipo)).toList();
            String detalle = deEsteTipo.size() + " · promedio " + promedioTexto(deEsteTipo);
            filas.add(filaConBarra(Util.icono(tipo), tipo, detalle, deEsteTipo.size(), maximo));
        }
        return filas;
    }

    private long cantidadDeTipo(List<Elemento> elementos, String tipo) {
        return elementos.stream().filter(e -> e.getTipo().equals(tipo)).count();
    }

    private Component crearPorGenero(List<Elemento> elementos) {
        Map<String, Long> porGenero = elementos.stream().collect(Collectors.groupingBy(
                e -> e.getGenero().trim(),
                () -> new TreeMap<String, Long>(String.CASE_INSENSITIVE_ORDER),
                Collectors.counting()));

        // De mayor a menor cantidad; si empatan, por nombre.
        List<Map.Entry<String, Long>> entradas = new ArrayList<>(porGenero.entrySet());
        entradas.sort((a, b) -> {
            int comparacion = Long.compare(b.getValue(), a.getValue());
            return comparacion != 0 ? comparacion : a.getKey().compareToIgnoreCase(b.getKey());
        });

        long maximo = entradas.isEmpty() ? 0 : entradas.get(0).getValue();
        VerticalLayout filas = nuevoContenedor();
        for (Map.Entry<String, Long> entrada : entradas) {
            filas.add(filaConBarra(null, entrada.getKey(), String.valueOf(entrada.getValue()), entrada.getValue(), maximo));
        }
        return filas;
    }

    private VerticalLayout nuevoContenedor() {
        VerticalLayout contenedor = new VerticalLayout();
        contenedor.setPadding(false);
        contenedor.setSpacing(false);
        contenedor.getStyle().set("gap", "0.6rem");
        return contenedor;
    }

    /** Una fila con etiqueta, valor y una barra cuyo ancho es proporcional al valor. */
    private Component filaConBarra(Component icono, String etiqueta, String detalle, long valor, long maximo) {
        HorizontalLayout nombre = new HorizontalLayout();
        nombre.setAlignItems(FlexComponent.Alignment.CENTER);
        nombre.setSpacing(true);
        if (icono != null) {
            nombre.add(icono);
        }
        nombre.add(new Span(etiqueta));

        HorizontalLayout cabecera = new HorizontalLayout(nombre, new Span(detalle));
        cabecera.setWidthFull();
        cabecera.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        double porcentaje = maximo == 0 ? 0 : valor * 100.0 / maximo;
        Div barra = new Div();
        barra.getStyle().set("height", "0.6rem")
                .set("border-radius", "0.3rem")
                .set("background", "var(--lumo-primary-color, #1676f3)")
                .set("width", String.format(Locale.ROOT, "%.0f%%", porcentaje));
        Div pista = new Div(barra);
        pista.setWidthFull();
        pista.getStyle().set("background", "var(--lumo-contrast-10pct, #e4e4e4)").set("border-radius", "0.3rem");

        Div fila = new Div(cabecera, pista);
        fila.setWidthFull();
        return fila;
    }

    private Component crearTopCalificados(List<Elemento> elementos) {
        List<Elemento> mejores = elementos.stream()
                .filter(e -> e.getCalificacion() > 0)
                .sorted(Comparator.comparingInt(Elemento::getCalificacion).reversed()
                        .thenComparing(Elemento::getTitulo, String.CASE_INSENSITIVE_ORDER))
                .limit(5)
                .toList();

        if (mejores.isEmpty()) {
            return new Span("Todavía no has calificado ningún elemento.");
        }

        Grid<Elemento> grid = new Grid<>(Elemento.class, false);
        grid.addColumn(Elemento::getTitulo).setHeader("Título").setAutoWidth(true).setFlexGrow(1);
        grid.addColumn(Elemento::getTipo).setHeader("Tipo").setAutoWidth(true);
        grid.addColumn(Elemento::getCreador).setHeader("Creador").setAutoWidth(true);
        grid.addComponentColumn(e -> {
            CalificacionEstrellas estrellas = new CalificacionEstrellas();
            estrellas.setValue(e.getCalificacion());
            estrellas.setReadOnly(true);
            return estrellas;
        }).setHeader("Calificación").setAutoWidth(true);
        grid.setItems(mejores);
        grid.setAllRowsVisible(true);
        return grid;
    }
}