package co.edu.coleccion.vistas;

import co.edu.coleccion.datos.Almacen;
import co.edu.coleccion.modelo.Elemento;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.Comparator;
import java.util.List;

/**
 * Vista principal: la colección completa en un Grid, con búsqueda en tiempo real
 * y filtros por tipo y por género. (Parte 1: solo lectura.)
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

        FlexLayout barra = new FlexLayout(buscar, filtroTipo, filtroGenero);
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
        grid.addColumn(e -> Util.estrellasTexto(e.getCalificacion())).setHeader("Calificación")
                .setComparator(Comparator.comparingInt(Elemento::getCalificacion)).setAutoWidth(true);
        grid.addColumn(e -> almacen.prestamoActivo(e.getId()).map(p -> "Prestado a " + p.getAmigo()).orElse("Disponible"))
                .setHeader("Estado").setSortable(true).setAutoWidth(true);
        grid.setSizeFull();
        grid.setMinHeight("20rem");
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