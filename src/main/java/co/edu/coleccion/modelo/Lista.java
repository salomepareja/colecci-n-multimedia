package co.edu.coleccion.modelo;

import java.util.ArrayList;
import java.util.List;

/**
 * Lista personalizada (por ejemplo "Para ver el fin de semana") con varios elementos.
 * Solo guarda los ids de los elementos.
 */
public class Lista {

    private int id;
    private String nombre;
    private List<Integer> elementoIds = new ArrayList<>();

    public Lista(int id, String nombre, List<Integer> elementoIds) {
        this.id = id;
        this.nombre = nombre;
        this.elementoIds = new ArrayList<>(elementoIds);
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public List<Integer> getElementoIds() { return elementoIds; }
    public void setElementoIds(List<Integer> elementoIds) { this.elementoIds = new ArrayList<>(elementoIds); }
}
