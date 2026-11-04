package co.edu.coleccion.vistas;

import co.edu.coleccion.datos.Almacen;
import co.edu.coleccion.modelo.Elemento;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.Comparator;
import java.util.List;

/**
 * Vista principal: la colección completa en un Grid, con búsqueda en tiempo real,
 * filtros por tipo y género, calificación con estrellas y acciones de modificar y eliminar.
 */
@Route(value = "", layout = MainLayout.class)
@PageTitle("Mi colección")
public class ColeccionView extends VerticalLayout {

    private final Almacen almacen = Almacen.get();

    private final Grid<Elemento> grid = new Grid<>(Elemento.class, false);
    private final TextField buscar = new TextField();
    private final ComboBox<String> filtroTipo = new ComboBox<>();
    private final ComboBox<String> filtroGenero = new ComboBox<>();
    private final Span contador = new Span();

    public ColeccionView() {
        setSizeFull();
        add(new H2("Mi colección"), crearBarraDeFiltros(), contador, grid);
        configurarGrid();
        actualizarLista();
    }

    private FlexLayout crearBarraDeFiltros() {
        buscar.setPlaceholder("Buscar por título o creador");
        buscar.setPrefixComponent(VaadinIcon.SEARCH.create());
        buscar.setClearButtonVisible(true);
        buscar.setValueChangeMode(ValueChangeMode.EAGER); // filtra mientras escribes
        buscar.setWidth("18rem");
        buscar.setMaxWidth("100%");
        buscar.addValueChangeListener(e -> actualizarLista());

        filtroTipo.setPlaceholder("Todos los tipos");
        filtroTipo.setItems(Elemento.TIPOS);
        filtroTipo.setClearButtonVisible(true);
        filtroTipo.addValueChangeListener(e -> actualizarLista());

        filtroGenero.setPlaceholder("Todos los géneros");
        filtroGenero.setItems(almacen.generos());
        filtroGenero.setClearButtonVisible(true);
        filtroGenero.addValueChangeListener(e -> actualizarLista());

        Button nuevo = new Button("Nuevo elemento", VaadinIcon.PLUS.create());
        nuevo.addThemeName("primary");
        nuevo.addClickListener(e -> UI.getCurrent().navigate("elemento"));

        FlexLayout barra = new FlexLayout(buscar, filtroTipo, filtroGenero, nuevo);
        barra.setFlexWrap(FlexLayout.FlexWrap.WRAP); // en el celular los campos bajan de renglón
        barra.getStyle().set("gap", "0.5rem");
        return barra;
    }

    private void configurarGrid() {
        grid.addColumn(Elemento::getTitulo).setHeader("Título").setSortable(true).setAutoWidth(true).setFlexGrow(1);
        grid.addColumn(Elemento::getTipo).setHeader("Tipo").setSortable(true).setAutoWidth(true);
        grid.addColumn(Elemento::getGenero).setHeader("Género").setSortable(true).setAutoWidth(true);
        grid.addColumn(Elemento::getAnio).setHeader("Año").setSortable(true).setAutoWidth(true);
        grid.addColumn(Elemento::getCreador).setHeader("Creador").setSortable(true).setAutoWidth(true);
        grid.addComponentColumn(this::crearEstrellas).setHeader("Calificación")
                .setComparator(Comparator.comparingInt(Elemento::getCalificacion)).setAutoWidth(true);
        grid.addColumn(e -> almacen.prestamoActivo(e.getId()).map(p -> "Prestado a " + p.getAmigo()).orElse("Disponible"))
                .setHeader("Estado").setSortable(true).setAutoWidth(true);
        grid.addComponentColumn(this::crearAcciones).setHeader("Acciones").setAutoWidth(true).setFlexGrow(0);
        grid.setSizeFull();
        grid.setMinHeight("20rem");
    }

    /** Estrellas clickeables dentro del Grid: al tocar una, la calificación se guarda de inmediato. */
    private Component crearEstrellas(Elemento elemento) {
        CalificacionEstrellas estrellas = new CalificacionEstrellas();
        estrellas.setValue(elemento.getCalificacion());
        estrellas.addValueChangeListener(evento -> {
            int anterior = elemento.getCalificacion();
            try {
                elemento.setCalificacion(evento.getValue());
                almacen.guardarElemento(elemento);
                Avisos.exito("Calificación guardada.");
            } catch (RuntimeException e) {
                elemento.setCalificacion(anterior);
                Avisos.error("No se pudo guardar la calificación: " + e.getMessage());
            }
        });
        return estrellas;
    }

    private Component crearAcciones(Elemento elemento) {
        Button editar = new Button(VaadinIcon.EDIT.create());
        editar.addThemeName("tertiary-inline");
        editar.setAriaLabel("Modificar " + elemento.getTitulo());
        editar.addClickListener(e -> UI.getCurrent().navigate("elemento/" + elemento.getId()));

        Button borrar = new Button(VaadinIcon.TRASH.create());
        borrar.addThemeName("tertiary-inline");
        borrar.addThemeName("error");
        borrar.setAriaLabel("Eliminar " + elemento.getTitulo());
        borrar.addClickListener(e -> pedirConfirmacionDeBorrado(elemento));

        return new HorizontalLayout(editar, borrar);
    }

    /** Toda eliminación pasa primero por un diálogo de confirmación. */
    private void pedirConfirmacionDeBorrado(Elemento elemento) {
        Avisos.confirmar("Eliminar elemento",
                "¿Seguro que quieres eliminar \"" + elemento.getTitulo() + "\"? "
                        + "También se borrará su historial de préstamos y saldrá de tus listas.",
                "Eliminar",
                () -> {
                    try {
                        almacen.eliminarElemento(elemento.getId());
                        Avisos.exito("Elemento eliminado.");
                        filtroGenero.setItems(almacen.generos());
                        actualizarLista();
                    } catch (RuntimeException e) {
                        Avisos.error(e.getMessage());
                    }
                });
    }

    /** Aplica la búsqueda y los filtros y vuelve a llenar el Grid. */
    private void actualizarLista() {
        String texto = buscar.getValue().trim().toLowerCase();
        List<Elemento> todos = almacen.getElementos();
        List<Elemento> filtrados = todos.stream()
                .filter(e -> texto.isEmpty()
                        || e.getTitulo().toLowerCase().contains(texto)
                        || e.getCreador().toLowerCase().contains(texto))
                .filter(e -> filtroTipo.isEmpty() || e.getTipo().equals(filtroTipo.getValue()))
                .filter(e -> filtroGenero.isEmpty() || e.getGenero().equalsIgnoreCase(filtroGenero.getValue()))
                .toList();
        grid.setItems(filtrados);
        contador.setText("Mostrando " + filtrados.size() + " de " + todos.size() + " elementos");
    }
}