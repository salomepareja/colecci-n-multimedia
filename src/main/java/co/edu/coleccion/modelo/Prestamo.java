package co.edu.coleccion.modelo;

import java.time.LocalDate;

/**
 * Préstamo de un elemento a un amigo. Mientras no tenga fecha de devolución, está activo.
 */
public class Prestamo {

    private int id;
    private int elementoId;
    private String amigo;
    private LocalDate fechaPrestamo;
    private LocalDate fechaDevolucion; // null = todavía no lo han devuelto

    public Prestamo(int id, int elementoId, String amigo, LocalDate fechaPrestamo, LocalDate fechaDevolucion) {
        this.id = id;
        this.elementoId = elementoId;
        this.amigo = amigo;
        this.fechaPrestamo = fechaPrestamo;
        this.fechaDevolucion = fechaDevolucion;
    }

    public boolean isActivo() {
        return fechaDevolucion == null;
    }

    public int getId() { return id; }
    public int getElementoId() { return elementoId; }
    public String getAmigo() { return amigo; }
    public LocalDate getFechaPrestamo() { return fechaPrestamo; }
    public LocalDate getFechaDevolucion() { return fechaDevolucion; }
    public void setFechaDevolucion(LocalDate fechaDevolucion) { this.fechaDevolucion = fechaDevolucion; }
}
