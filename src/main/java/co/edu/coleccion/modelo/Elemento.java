package co.edu.coleccion.modelo;

import java.util.List;

/**
 * Un elemento de la colección: una película, una serie o un álbum/canción.
 * La calificación va de 1 a 5; el valor 0 significa "sin calificar".
 */
public class Elemento {

    public static final String PELICULA = "Película";
    public static final String SERIE = "Serie";
    public static final String MUSICA = "Música";
    public static final List<String> TIPOS = List.of(PELICULA, SERIE, MUSICA);

    private int id;
    private String titulo;
    private String tipo;
    private String genero;
    private int anio;
    private String creador; // director o artista
    private int calificacion;

    public Elemento() {
        this.titulo = "";
        this.tipo = PELICULA;
        this.genero = "";
        this.creador = "";
    }

    public Elemento(int id, String titulo, String tipo, String genero, int anio, String creador, int calificacion) {
        this.id = id;
        this.titulo = titulo;
        this.tipo = tipo;
        this.genero = genero;
        this.anio = anio;
        this.creador = creador;
        this.calificacion = calificacion;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getGenero() { return genero; }
    public void setGenero(String genero) { this.genero = genero; }

    public int getAnio() { return anio; }
    public void setAnio(int anio) { this.anio = anio; }

    public String getCreador() { return creador; }
    public void setCreador(String creador) { this.creador = creador; }

    public int getCalificacion() { return calificacion; }
    public void setCalificacion(int calificacion) { this.calificacion = calificacion; }

    // Dos elementos son el mismo si tienen el mismo id (lo necesitan los ComboBox y el Grid).
    @Override
    public boolean equals(Object otro) {
        return otro instanceof Elemento e && e.id == this.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }

    @Override
    public String toString() {
        return titulo;
    }
}
