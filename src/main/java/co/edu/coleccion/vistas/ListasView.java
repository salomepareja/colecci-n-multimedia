package co.edu.coleccion.vistas;

import co.edu.coleccion.datos.Almacen;
import co.edu.coleccion.modelo.Elemento;
import co.edu.coleccion.modelo.Lista;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Listas personalizadas (por ejemplo "Para ver el fin de semana").
 * Con un MultiSelectComboBox se agregan varios elementos a la lista de una sola vez.
 */
@Route(value = "listas", layout = MainLayout.class)
@PageTitle("Listas")
public class ListasView extends VerticalLayout {

    private final Almacen almacen = Almacen.get();

    private final H3 subtitulo = new H3("Nueva lista");
    private final TextField nombre = new TextField("Nombre de la lista");
    private final MultiSelectComboBox<Elemento> elementos = new MultiSelectComboBox<>("Elementos");
    private final Button guardar = new Button("Guardar lista", VaadinIcon.CHECK.create());
    private final Button cancelarEdicion = new Button("Cancelar edición");
    private final Grid<Lista> grid = new Grid<>(Lista.class, false);

    private Lista enEdicion; // null cuando se está creando una lista nueva

    public ListasView() {
        setSizeFull();
        add(new H2("Mis listas"), subtitulo, crearFormulario(), grid);
        configurarGrid();
        refrescar();
    }

    private Component crearFormulario() {
        nombre.setRequiredIndicatorVisible(true);
        nombre.setMaxLength(60);
        nombre.setWidth("18rem");
        nombre.setMaxWidth("100%");

        elementos.setItemLabelGenerator(e -> e.getTitulo() + " (" + e.getTipo() + ")");
        elementos.setRequiredIndicatorVisible(true);
        elementos.setWidth("24rem");
        elementos.setMaxWidth("100%");

        guardar.addThemeName("primary");
        guardar.addClickListener(e -> guardarLista());

        cancelarEdicion.setVisible(false);
        cancelarEdicion.addClickListener(e -> limpiarFormulario());

        FlexLayout fila = new FlexLayout(nombre, elementos, guardar, cancelarEdicion);
        fila.setFlexWrap(FlexLayout.FlexWrap.WRAP);
        fila.setAlignItems(FlexComponent.Alignment.END);
        fila.getStyle().set("gap", "0.5rem");
        return fila;
    }

    private void configurarGrid() {
        grid.addColumn(Lista::getNombre).setHeader("Lista").setSortable(true).setAutoWidth(true);
        grid.addColumn(l -> l.getElementoIds().size()).setHeader("Cantidad").setAutoWidth(true);
        grid.addColumn(this::textoDeElementos).setHeader("Elementos").setAutoWidth(true).setFlexGrow(1);
        grid.addComponentColumn(this::crearAcciones).setHeader("Acciones").setAutoWidth(true).setFlexGrow(0);
        grid.setSizeFull();
        grid.setMinHeight("14rem");
    }

    private String textoDeElementos(Lista lista) {
        return lista.getElementoIds().stream()
                .map(id -> almacen.buscarElemento(id).map(Elemento::getTitulo).orElse(null))
                .filter(titulo -> titulo != null)
                .collect(Collectors.joining(", "));
    }

    private Component crearAcciones(Lista lista) {
        Button editar = new Button(VaadinIcon.EDIT.create());
        editar.addThemeName("tertiary-inline");
        editar.setAriaLabel("Modificar " + lista.getNombre());
        editar.addClickListener(e -> cargarParaEditar(lista));

        Button borrar = new Button(VaadinIcon.TRASH.create());
        borrar.addThemeName("tertiary-inline");
        borrar.addThemeName("error");
        borrar.setAriaLabel("Eliminar " + lista.getNombre());
        borrar.addClickListener(e -> Avisos.confirmar("Eliminar lista",
                "¿Seguro que quieres eliminar la lista \"" + lista.getNombre() + "\"? "
                        + "Los elementos no se borran, solo la lista.",
                "Eliminar",
                () -> eliminar(lista)));

        return new HorizontalLayout(editar, borrar);
    }

    private void cargarParaEditar(Lista lista) {
        enEdicion = lista;
        subtitulo.setText("Modificar lista");
        nombre.setValue(lista.getNombre());
        Set<Elemento> seleccionados = new HashSet<>();
        for (int id : lista.getElementoIds()) {
            almacen.buscarElemento(id).ifPresent(seleccionados::add);
        }
        elementos.setValue(seleccionados);
        cancelarEdicion.setVisible(true);
    }

    private void guardarLista() {
        String nombreLista = nombre.getValue().trim();
        if (nombreLista.isEmpty()) {
            Avisos.error("Escribe un nombre para la lista.");
            return;
        }
        if (elementos.isEmpty()) {
            Avisos.error("Elige al menos un elemento para la lista.");
            return;
        }
        int idActual = enEdicion == null ? 0 : enEdicion.getId();
        boolean repetido = almacen.getListas().stream()
                .anyMatch(l -> l.getId() != idActual && l.getNombre().equalsIgnoreCase(nombreLista));
        if (repetido) {
            Avisos.error("Ya existe una lista con ese nombre.");
            return;
        }

        List<Integer> ids = new ArrayList<>();
        for (Elemento e : elementos.getValue()) {
            ids.add(e.getId());
        }
        try {
            almacen.guardarLista(new Lista(idActual, nombreLista, ids));
            Avisos.exito(enEdicion == null ? "Lista creada." : "Lista actualizada.");
            limpiarFormulario();
        } catch (RuntimeException e) {
            Avisos.error("No se pudo guardar: " + e.getMessage());
        }
    }

    private void eliminar(Lista lista) {
        try {
            almacen.eliminarLista(lista.getId());
            Avisos.exito("Lista eliminada.");
            if (enEdicion != null && enEdicion.getId() == lista.getId()) {
                limpiarFormulario();
            }
        } catch (RuntimeException e) {
            Avisos.error(e.getMessage());
        }
        refrescar();
    }

    private void limpiarFormulario() {
        enEdicion = null;
        subtitulo.setText("Nueva lista");
        nombre.clear();
        elementos.clear();
        cancelarEdicion.setVisible(false);
        refrescar();
    }

    private void refrescar() {
        elementos.setItems(almacen.getElementos());
        grid.setItems(almacen.getListas());
    }
}