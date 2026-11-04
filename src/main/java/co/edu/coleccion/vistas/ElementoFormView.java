package co.edu.coleccion.vistas;

import co.edu.coleccion.datos.Almacen;
import co.edu.coleccion.modelo.Elemento;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Formulario para crear o modificar un elemento.
 *   /elemento      -> elemento nuevo
 *   /elemento/7    -> modificar el elemento con id 7
 * El ":id?" de la ruta es un parámetro opcional.
 */
@Route(value = "elemento/:id?", layout = MainLayout.class)
@PageTitle("Elemento")
public class ElementoFormView extends VerticalLayout implements BeforeEnterObserver {

    private static final int ANIO_MINIMO = 1850;

    private final Almacen almacen = Almacen.get();

    private final H2 encabezado = new H2("Nuevo elemento");
    private final TextField titulo = new TextField("Título");
    private final TextField creador = new TextField("Director o artista");
    private final ComboBox<String> tipo = new ComboBox<>("Tipo");
    private final ComboBox<String> genero = new ComboBox<>("Género");
    private final IntegerField anio = new IntegerField("Año");
    private final CalificacionEstrellas calificacion = new CalificacionEstrellas();

    private Elemento actual; // null cuando se está creando uno nuevo

    public ElementoFormView() {
        setMaxWidth("48rem");

        titulo.setRequiredIndicatorVisible(true);
        titulo.setMaxLength(100);

        creador.setRequiredIndicatorVisible(true);
        creador.setMaxLength(100);

        tipo.setItems(Elemento.TIPOS);
        tipo.setRequiredIndicatorVisible(true);
        tipo.setAllowCustomValue(false);

        genero.setItems(almacen.generos());
        genero.setRequiredIndicatorVisible(true);
        genero.setAllowCustomValue(true); // se puede escribir un género nuevo
        genero.setHelperText("Elige uno o escribe uno nuevo y pulsa Enter");
        genero.addCustomValueSetListener(evento -> genero.setValue(evento.getDetail()));

        anio.setRequiredIndicatorVisible(true);
        anio.setStepButtonsVisible(true);
        anio.setMin(ANIO_MINIMO);
        anio.setMax(LocalDate.now().getYear() + 1);

        calificacion.setLabel("Calificación (toca una estrella; tócala de nuevo para quitarla)");

        FormLayout formulario = new FormLayout();
        formulario.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("32rem", 2)); // una columna en el celular, dos en pantallas anchas
        formulario.add(titulo, creador, tipo, genero, anio, calificacion);
        formulario.setColspan(titulo, 2);

        Button guardar = new Button("Guardar", VaadinIcon.CHECK.create());
        guardar.addThemeName("primary");
        guardar.addClickListener(evento -> guardar());

        Button cancelar = new Button("Cancelar");
        cancelar.addClickListener(evento -> volver());

        FlexLayout botones = new FlexLayout(guardar, cancelar);
        botones.getStyle().set("gap", "0.5rem");

        add(encabezado, formulario, botones);
    }

    /** Se ejecuta al entrar a la vista: si trae un id, carga ese elemento en el formulario. */
    @Override
    public void beforeEnter(BeforeEnterEvent evento) {
        Optional<String> idTexto = evento.getRouteParameters().get("id");
        if (idTexto.isEmpty()) {
            tipo.setValue(Elemento.PELICULA);
            return;
        }
        try {
            int id = Integer.parseInt(idTexto.get());
            Optional<Elemento> encontrado = almacen.buscarElemento(id);
            if (encontrado.isEmpty()) {
                throw new IllegalArgumentException("no existe");
            }
            actual = encontrado.get();
            encabezado.setText("Modificar elemento");
            titulo.setValue(actual.getTitulo());
            creador.setValue(actual.getCreador());
            tipo.setValue(actual.getTipo());
            genero.setValue(actual.getGenero());
            anio.setValue(actual.getAnio());
            calificacion.setValue(actual.getCalificacion());
        } catch (IllegalArgumentException e) {
            Avisos.error("Ese elemento no existe.");
            evento.forwardTo(ColeccionView.class);
        }
    }

    private void guardar() {
        List<String> errores = validar();
        if (!errores.isEmpty()) {
            Avisos.error(String.join(" ", errores));
            return;
        }

        // Se arma un elemento nuevo; si falla algo, el original queda intacto.
        Elemento elemento = new Elemento(
                actual == null ? 0 : actual.getId(),
                titulo.getValue().trim(),
                tipo.getValue(),
                genero.getValue().trim(),
                anio.getValue(),
                creador.getValue().trim(),
                calificacion.getValue());
        try {
            almacen.guardarElemento(elemento);
            Avisos.exito(actual == null ? "Elemento agregado a tu colección." : "Cambios guardados.");
            volver();
        } catch (RuntimeException e) {
            Avisos.error("No se pudo guardar: " + e.getMessage());
        }
    }

    /** Revisa todos los campos antes de guardar y devuelve los mensajes de lo que esté mal. */
    private List<String> validar() {
        List<String> errores = new ArrayList<>();
        if (titulo.getValue().isBlank()) {
            errores.add("El título es obligatorio.");
        }
        if (creador.getValue().isBlank()) {
            errores.add("Escribe el director o artista.");
        }
        if (tipo.isEmpty()) {
            errores.add("Elige el tipo.");
        }
        if (genero.isEmpty() || genero.getValue().isBlank()) {
            errores.add("El género es obligatorio.");
        }
        int anioMaximo = LocalDate.now().getYear() + 1;
        if (anio.isEmpty() || anio.getValue() < ANIO_MINIMO || anio.getValue() > anioMaximo) {
            errores.add("El año debe estar entre " + ANIO_MINIMO + " y " + anioMaximo + ".");
        }
        if (errores.isEmpty() && existeDuplicado()) {
            errores.add("Ya tienes un elemento igual (mismo título, tipo y creador).");
        }
        return errores;
    }

    private boolean existeDuplicado() {
        int idActual = actual == null ? 0 : actual.getId();
        return almacen.getElementos().stream().anyMatch(e -> e.getId() != idActual
                && e.getTitulo().equalsIgnoreCase(titulo.getValue().trim())
                && e.getTipo().equals(tipo.getValue())
                && e.getCreador().equalsIgnoreCase(creador.getValue().trim()));
    }

    private void volver() {
        UI.getCurrent().navigate(ColeccionView.class);
    }
}
