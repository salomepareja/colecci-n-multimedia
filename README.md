# Colección multimedia

Aplicación web hecha con Vaadin para organizar una colección de películas, series y música.

Proyecto final de Lógica de Programación.

- SALOME PAREJA PARRA
- fecha: 10/28/2026

## Qué hice

- Creé el proyecto con Spring Initializr (Java 21 y Vaadin).
- Organicé el código en tres paquetes: `modelo`, `datos` y `vistas`.
- Hice las clases del modelo: `Elemento`, `Prestamo` y `Lista`.
- Guardé los datos en archivos `.txt` con los campos separados por `|`, usando la clase `Almacen` para leerlos y escribirlos. Si un archivo falta o una línea está mal escrita, la aplicación no se cae.
- Agregué datos de ejemplo que se crean solos la primera vez que corre la aplicación.
- Armé un menú lateral con `AppLayout`.
- Hice la vista de la colección: una tabla con búsqueda en tiempo real por título o creador y filtros por tipo y género.

## Tecnologías

Java 21, Vaadin, Spring Boot y Maven. Los datos se guardan en archivos de texto, sin base de datos.

## Cómo ejecutarla

1. Abrir el proyecto en VS Code.
2. Ejecutar `ColeccionApplication.java`.
3. Abrir <http://localhost:8080> en el navegador.

La primera vez se crea la carpeta `datos/` con información de ejemplo.



11/4/2026
- Hice el formulario para agregar y modificar elementos, con validaciones: los campos obligatorios, el año entre 1850 y el año siguiente, y que no se repita un elemento con el mismo título, tipo y creador.
- Hice un componente propio de calificación con 5 estrellas (`Icon`). Se usa en el formulario y en la tabla, donde al tocar una estrella la calificación se guarda al instante.
- Agregué los botones de modificar y eliminar en la tabla. Eliminar pide confirmación, y un elemento que está prestado no se puede eliminar.
- Hice la vista de préstamos: se registra a quién se le presta un elemento y en qué fecha, y se registra su devolución. Un elemento ya prestado no se puede volver a prestar, y el historial se puede filtrar por estado.