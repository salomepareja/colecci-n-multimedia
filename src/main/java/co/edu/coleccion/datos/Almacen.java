package co.edu.coleccion.datos;

import co.edu.coleccion.modelo.Elemento;
import co.edu.coleccion.modelo.Lista;
import co.edu.coleccion.modelo.Prestamo;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.IntStream;

/**
 * Clase que lee y escribe los archivos de texto (carpeta "datos").
 * Cada tipo de dato vive en su propio .txt, con los campos separados por |
 *
 *   elementos.txt : id|titulo|tipo|genero|anio|creador|calificacion
 *   prestamos.txt : id|elementoId|amigo|fechaPrestamo|fechaDevolucion
 *   listas.txt    : id|nombre|id1,id2,id3
 *
 * Los datos se cargan una sola vez y se mantienen en memoria; cada cambio se guarda
 * en el archivo de inmediato, así nada se pierde al reiniciar la aplicación.
 */
public class Almacen {

    private static final Almacen INSTANCIA = new Almacen();

    /** Todas las vistas usan la misma instancia, así comparten los mismos datos. */
    public static Almacen get() {
        return INSTANCIA;
    }

    private final Path carpeta = Path.of("datos");
    private final Path archivoElementos = carpeta.resolve("elementos.txt");
    private final Path archivoPrestamos = carpeta.resolve("prestamos.txt");
    private final Path archivoListas = carpeta.resolve("listas.txt");

    private List<Elemento> elementos = new ArrayList<>();
    private List<Prestamo> prestamos = new ArrayList<>();
    private List<Lista> listas = new ArrayList<>();

    private Almacen() {
        cargar();
    }

    // ------------------------------------------------------------------
    // Carga inicial
    // ------------------------------------------------------------------

    private void cargar() {
        try {
            Files.createDirectories(carpeta);
        } catch (IOException e) {
            System.err.println("No se pudo crear la carpeta de datos: " + e.getMessage());
        }

        if (!Files.exists(archivoElementos)) {
            crearDatosDeEjemplo();
            return;
        }
        elementos = leer(archivoElementos, this::convertirElemento);
        prestamos = leer(archivoPrestamos, this::convertirPrestamo);
        listas = leer(archivoListas, this::convertirLista);
    }

    /** Lee un archivo línea por línea. Un archivo que falta o una línea mal escrita no tumban la aplicación. */
    private <T> List<T> leer(Path archivo, Function<String[], T> conversor) {
        List<T> resultado = new ArrayList<>();
        if (!Files.exists(archivo)) {
            return resultado;
        }
        List<String> lineas;
        try {
            lineas = Files.readAllLines(archivo, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("No se pudo leer " + archivo + ": " + e.getMessage());
            return resultado;
        }
        int numero = 0;
        for (String linea : lineas) {
            numero++;
            if (linea.isBlank()) {
                continue;
            }
            try {
                resultado.add(conversor.apply(linea.split("\\|", -1)));
            } catch (Exception e) {
                System.err.println("Línea " + numero + " ignorada en " + archivo.getFileName() + ": " + e);
            }
        }
        return resultado;
    }

    private Elemento convertirElemento(String[] p) {
        int calificacion = Integer.parseInt(p[6].trim());
        if (p[1].isBlank() || calificacion < 0 || calificacion > 5) {
            throw new IllegalArgumentException("datos inválidos");
        }
        return new Elemento(Integer.parseInt(p[0].trim()), p[1].trim(), p[2].trim(), p[3].trim(),
                Integer.parseInt(p[4].trim()), p[5].trim(), calificacion);
    }

    private Prestamo convertirPrestamo(String[] p) {
        LocalDate devolucion = p[4].isBlank() ? null : LocalDate.parse(p[4].trim());
        return new Prestamo(Integer.parseInt(p[0].trim()), Integer.parseInt(p[1].trim()), p[2].trim(),
                LocalDate.parse(p[3].trim()), devolucion);
    }

    private Lista convertirLista(String[] p) {
        List<Integer> ids = new ArrayList<>();
        if (!p[2].isBlank()) {
            for (String texto : p[2].split(",")) {
                ids.add(Integer.parseInt(texto.trim()));
            }
        }
        return new Lista(Integer.parseInt(p[0].trim()), p[1].trim(), ids);
    }

    // ------------------------------------------------------------------
    // Escritura
    // ------------------------------------------------------------------

    private void escribir(Path archivo, List<String> lineas) {
        try {
            Files.createDirectories(carpeta);
            Files.write(archivo, lineas, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar el archivo " + archivo.getFileName(), e);
        }
    }

    /** Evita que un texto con | o con saltos de línea dañe el formato del archivo. */
    private String limpiar(String texto) {
        return texto == null ? "" : texto.replace('|', '/').replace('\n', ' ').replace('\r', ' ').trim();
    }

    private void guardarElementos() {
        escribir(archivoElementos, elementos.stream()
                .map(e -> String.join("|", String.valueOf(e.getId()), limpiar(e.getTitulo()), limpiar(e.getTipo()),
                        limpiar(e.getGenero()), String.valueOf(e.getAnio()), limpiar(e.getCreador()),
                        String.valueOf(e.getCalificacion())))
                .toList());
    }

    private void guardarPrestamos() {
        escribir(archivoPrestamos, prestamos.stream()
                .map(p -> String.join("|", String.valueOf(p.getId()), String.valueOf(p.getElementoId()),
                        limpiar(p.getAmigo()), p.getFechaPrestamo().toString(),
                        p.getFechaDevolucion() == null ? "" : p.getFechaDevolucion().toString()))
                .toList());
    }

    private void guardarListas() {
        escribir(archivoListas, listas.stream()
                .map(l -> String.join("|", String.valueOf(l.getId()), limpiar(l.getNombre()),
                        String.join(",", l.getElementoIds().stream().map(String::valueOf).toList())))
                .toList());
    }

    private int siguienteId(IntStream ids) {
        return ids.max().orElse(0) + 1;
    }

    // ------------------------------------------------------------------
    // Elementos
    // ------------------------------------------------------------------

    /** Devuelve una copia ordenada por título. */
    public synchronized List<Elemento> getElementos() {
        List<Elemento> copia = new ArrayList<>(elementos);
        copia.sort(Comparator.comparing(Elemento::getTitulo, String.CASE_INSENSITIVE_ORDER));
        return copia;
    }

    public synchronized Optional<Elemento> buscarElemento(int id) {
        return elementos.stream().filter(e -> e.getId() == id).findFirst();
    }

    /** Crea el elemento (si su id es 0) o reemplaza el que tiene ese mismo id. */
    public synchronized void guardarElemento(Elemento nuevo) {
        if (nuevo.getId() == 0) {
            nuevo.setId(siguienteId(elementos.stream().mapToInt(Elemento::getId)));
            elementos.add(nuevo);
        } else {
            boolean reemplazado = false;
            for (int i = 0; i < elementos.size(); i++) {
                if (elementos.get(i).getId() == nuevo.getId()) {
                    elementos.set(i, nuevo);
                    reemplazado = true;
                    break;
                }
            }
            if (!reemplazado) {
                elementos.add(nuevo);
            }
        }
        guardarElementos();
    }

    /** Un elemento prestado no se puede eliminar. Al borrarlo también salen sus préstamos y sus listas. */
    public synchronized void eliminarElemento(int id) {
        if (estaPrestado(id)) {
            throw new IllegalStateException("No se puede eliminar: el elemento está prestado. Registra primero su devolución.");
        }
        elementos.removeIf(e -> e.getId() == id);
        prestamos.removeIf(p -> p.getElementoId() == id);
        for (Lista lista : listas) {
            lista.getElementoIds().remove(Integer.valueOf(id));
        }
        guardarElementos();
        guardarPrestamos();
        guardarListas();
    }

    /** Géneros que existen hoy en la colección, sin repetir y ordenados. */
    public synchronized List<String> generos() {
        TreeSet<String> generos = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (Elemento e : elementos) {
            if (!e.getGenero().isBlank()) {
                generos.add(e.getGenero());
            }
        }
        return new ArrayList<>(generos);
    }

    // ------------------------------------------------------------------
    // Préstamos
    // ------------------------------------------------------------------

    public synchronized Optional<Prestamo> prestamoActivo(int elementoId) {
        return prestamos.stream().filter(p -> p.getElementoId() == elementoId && p.isActivo()).findFirst();
    }

    public synchronized boolean estaPrestado(int elementoId) {
        return prestamoActivo(elementoId).isPresent();
    }

    /** Todos los préstamos, los más recientes primero. */
    public synchronized List<Prestamo> getPrestamos() {
        List<Prestamo> copia = new ArrayList<>(prestamos);
        copia.sort(Comparator.comparing(Prestamo::getFechaPrestamo).thenComparing(Prestamo::getId).reversed());
        return copia;
    }

    public synchronized List<Prestamo> prestamosDe(int elementoId) {
        return getPrestamos().stream().filter(p -> p.getElementoId() == elementoId).toList();
    }

    public synchronized Prestamo prestar(int elementoId, String amigo, LocalDate fecha) {
        if (buscarElemento(elementoId).isEmpty()) {
            throw new IllegalArgumentException("El elemento no existe.");
        }
        if (estaPrestado(elementoId)) {
            throw new IllegalStateException("Ese elemento ya está prestado; primero debe devolverse.");
        }
        String nombre = limpiar(amigo);
        if (nombre.isEmpty()) {
            throw new IllegalArgumentException("Escribe a quién se lo prestas.");
        }
        Prestamo prestamo = new Prestamo(siguienteId(prestamos.stream().mapToInt(Prestamo::getId)),
                elementoId, nombre, fecha, null);
        prestamos.add(prestamo);
        guardarPrestamos();
        return prestamo;
    }

    public synchronized void devolver(int prestamoId, LocalDate fecha) {
        Prestamo prestamo = prestamos.stream().filter(p -> p.getId() == prestamoId).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("El préstamo no existe."));
        if (!prestamo.isActivo()) {
            throw new IllegalStateException("Ese préstamo ya fue devuelto.");
        }
        if (fecha.isBefore(prestamo.getFechaPrestamo())) {
            throw new IllegalArgumentException("La devolución no puede ser anterior al préstamo.");
        }
        prestamo.setFechaDevolucion(fecha);
        guardarPrestamos();
    }

    // ------------------------------------------------------------------
    // Listas personalizadas
    // ------------------------------------------------------------------

    public synchronized List<Lista> getListas() {
        List<Lista> copia = new ArrayList<>(listas);
        copia.sort(Comparator.comparing(Lista::getNombre, String.CASE_INSENSITIVE_ORDER));
        return copia;
    }

    public synchronized void guardarLista(Lista nueva) {
        if (nueva.getId() == 0) {
            nueva.setId(siguienteId(listas.stream().mapToInt(Lista::getId)));
            listas.add(nueva);
        } else {
            boolean reemplazada = false;
            for (int i = 0; i < listas.size(); i++) {
                if (listas.get(i).getId() == nueva.getId()) {
                    listas.set(i, nueva);
                    reemplazada = true;
                    break;
                }
            }
            if (!reemplazada) {
                listas.add(nueva);
            }
        }
        guardarListas();
    }

    public synchronized void eliminarLista(int id) {
        listas.removeIf(l -> l.getId() == id);
        guardarListas();
    }

    // ------------------------------------------------------------------
    // Datos de ejemplo (se crean solo la primera vez que corre la aplicación)
    // ------------------------------------------------------------------

    private void crearDatosDeEjemplo() {
        elementos = new ArrayList<>(List.of(
                new Elemento(1, "Inception", Elemento.PELICULA, "Ciencia ficción", 2010, "Christopher Nolan", 5),
                new Elemento(2, "El Padrino", Elemento.PELICULA, "Drama", 1972, "Francis Ford Coppola", 5),
                new Elemento(3, "Amélie", Elemento.PELICULA, "Comedia", 2001, "Jean-Pierre Jeunet", 4),
                new Elemento(4, "Coco", Elemento.PELICULA, "Animación", 2017, "Lee Unkrich", 4),
                new Elemento(5, "Breaking Bad", Elemento.SERIE, "Drama", 2008, "Vince Gilligan", 5),
                new Elemento(6, "Stranger Things", Elemento.SERIE, "Ciencia ficción", 2016, "Hermanos Duffer", 4),
                new Elemento(7, "The Office", Elemento.SERIE, "Comedia", 2005, "Greg Daniels", 3),
                new Elemento(8, "Dark", Elemento.SERIE, "Ciencia ficción", 2017, "Baran bo Odar", 5),
                new Elemento(9, "Abbey Road", Elemento.MUSICA, "Rock", 1969, "The Beatles", 5),
                new Elemento(10, "Thriller", Elemento.MUSICA, "Pop", 1982, "Michael Jackson", 4),
                new Elemento(11, "Kind of Blue", Elemento.MUSICA, "Jazz", 1959, "Miles Davis", 5),
                new Elemento(12, "Pies descalzos", Elemento.MUSICA, "Pop", 1995, "Shakira", 0)));

        prestamos = new ArrayList<>(List.of(
                new Prestamo(1, 2, "Camila", LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 20)),
                new Prestamo(2, 5, "Andrés", LocalDate.of(2026, 10, 22), null),
                new Prestamo(3, 9, "Laura", LocalDate.of(2026, 10, 25), null),
                new Prestamo(4, 1, "Andrés", LocalDate.of(2026, 9, 15), LocalDate.of(2026, 9, 30))));

        listas = new ArrayList<>(List.of(
                new Lista(1, "Para ver el fin de semana", List.of(1, 3, 6)),
                new Lista(2, "Música para estudiar", List.of(9, 11))));

        try {
            guardarElementos();
            guardarPrestamos();
            guardarListas();
        } catch (UncheckedIOException e) {
            System.err.println(e.getMessage());
        }
    }
}
