package co.edu.coleccion.vistas;

import co.edu.coleccion.datos.Almacen;
import co.edu.coleccion.modelo.Elemento;
import co.edu.coleccion.modelo.Prestamo;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/**
 * Préstamos a amigos: registrar un préstamo (elemento + amigo + fecha) y registrar la devolución.
 * Un elemento que ya está prestado no aparece en la lista para prestar.
 */
@Route(value = "prestamos", layout = MainLayout.class)
@PageTitle("Préstamos")
public class PrestamosView extends VerticalLayout {

    private static final String TODOS = "Todos";
    private static final String EN_PRESTAMO = "En préstamo";
    private static final String DEVUELTOS = "Devueltos";

    private final Almacen almacen = Almacen.get();

    private final ComboBox<Elemento> elemento = new ComboBox<>("Elemento a prestar");
    private final TextField amigo = new TextField("Amigo");
    private final DatePicker fecha = new DatePicker("Fecha del préstamo");
    private final Select<String> filtroEstado = new Select<>();
    private final Grid<Prestamo> grid = new Grid<>(Prestamo.class, false);

    public PrestamosView() {
        setSizeFull();
        add(new H2("Préstamos"), crearFormulario(), new H3("Historial de préstamos"), crearFiltro(), grid);
        configurarGrid();
        refrescar();
    }

    private Component crearFormulario() {
        elemento.setItemLabelGenerator(e -> e.getTitulo() + " (" + e.getTipo() + ")");
        elemento.setRequiredIndicatorVisible(true);
        elemento.setWidth("20rem");
        elemento.setMaxWidth("100%");

        amigo.setRequiredIndicatorVisible(true);
        amigo.setMaxLength(60);

        fecha.setLocale(Util.LOCALE_ES);
        fecha.setValue(LocalDate.now());
        fecha.setMax(LocalDate.now()); // no se puede registrar un préstamo en el futuro

        Button prestar = new Button("Registrar préstamo", VaadinIcon.PLUS.create());
        prestar.addThemeName("primary");
        prestar.addClickListener(e -> registrarPrestamo());

        FlexLayout fila = new FlexLayout(elemento, amigo, fecha, prestar);
        fila.setFlexWrap(FlexLayout.FlexWrap.WRAP);
        fila.setAlignItems(FlexComponent.Alignment.END);
        fila.getStyle().set("gap", "0.5rem");
        return fila;
    }

    private Component crearFiltro() {
        filtroEstado.setLabel("Mostrar");
        filtroEstado.setItems(TODOS, EN_PRESTAMO, DEVUELTOS);
        filtroEstado.setValue(TODOS);
        filtroEstado.addValueChangeListener(e -> refrescarTabla());
        return filtroEstado;
    }

    private void configurarGrid() {
        grid.addColumn(p -> almacen.buscarElemento(p.getElementoId()).map(Elemento::getTitulo).orElse("(eliminado)"))
                .setHeader("Elemento").setAutoWidth(true).setFlexGrow(1);
        grid.addColumn(Prestamo::getAmigo).setHeader("Amigo").setAutoWidth(true);
        grid.addColumn(p -> Util.fecha(p.getFechaPrestamo())).setHeader("Prestado el")
                .setComparator(Comparator.comparing(Prestamo::getFechaPrestamo)).setAutoWidth(true);
        grid.addColumn(p -> p.isActivo() ? "En préstamo" : "Devuelto el " + Util.fecha(p.getFechaDevolucion()))
                .setHeader("Estado").setAutoWidth(true);
        grid.addComponentColumn(this::crearBotonDevolver).setHeader("").setAutoWidth(true).setFlexGrow(0);
        grid.setSizeFull();
        grid.setMinHeight("16rem");
    }

    private Component crearBotonDevolver(Prestamo prestamo) {
        if (!prestamo.isActivo()) {
            return new Span();
        }
        Button devolver = new Button("Devolver", VaadinIcon.CHECK.create());
        devolver.addThemeName("small");
        devolver.addClickListener(e -> {
            String titulo = almacen.buscarElemento(prestamo.getElementoId()).map(Elemento::getTitulo).orElse("el elemento");
            Avisos.confirmar("Registrar devolución",
                    "¿Confirmas que " + prestamo.getAmigo() + " ya devolvió \"" + titulo + "\"?",
                    "Registrar devolución",
                    () -> devolverPrestamo(prestamo));
        });
        return devolver;
    }

    private void registrarPrestamo() {
        if (elemento.isEmpty()) {
            Avisos.error("Elige el elemento que vas a prestar.");
            return;
        }
        if (amigo.getValue().isBlank()) {
            Avisos.error("Escribe a quién se lo prestas.");
            amigo.focus();
            return;
        }
        LocalDate dia = fecha.getValue();
        if (dia == null) {
            Avisos.error("Elige la fecha del préstamo.");
            return;
        }
        if (dia.isAfter(LocalDate.now())) {
            Avisos.error("La fecha del préstamo no puede ser futura.");
            return;
        }
        String mensaje = "\"" + elemento.getValue().getTitulo() + "\" prestado a " + amigo.getValue().trim() + ".";
        try {
            almacen.prestar(elemento.getValue().getId(), amigo.getValue(), dia);
            Avisos.exito(mensaje);
            elemento.clear();
            amigo.clear();
            fecha.setValue(LocalDate.now());
        } catch (RuntimeException e) {
            Avisos.error(e.getMessage());
        }
        refrescar();
    }

    private void devolverPrestamo(Prestamo prestamo) {
        try {
            almacen.devolver(prestamo.getId(), LocalDate.now());
            Avisos.exito("Devolución registrada.");
        } catch (RuntimeException e) {
            Avisos.error(e.getMessage());
        }
        refrescar();
    }

    /** Actualiza la lista de elementos disponibles y la tabla. */
    private void refrescar() {
        List<Elemento> disponibles = almacen.getElementos().stream()
                .filter(e -> !almacen.estaPrestado(e.getId()))
                .toList();
        elemento.setItems(disponibles);
        refrescarTabla();
    }

    private void refrescarTabla() {
        String estado = filtroEstado.getValue();
        List<Prestamo> lista = almacen.getPrestamos().stream()
                .filter(p -> EN_PRESTAMO.equals(estado) ? p.isActivo()
                        : DEVUELTOS.equals(estado) ? !p.isActivo() : true)
                .toList();
        grid.setItems(lista);
    }
}