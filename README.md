# Registro de Personas — App Android con SQLite

Aplicación Android nativa (Java) que permite **guardar, buscar, actualizar, eliminar y listar** personas (nombre y apellido) usando una base de datos **SQLite local**. Proyecto de práctica para el curso de Lenguajes de Programación, hecho en Android Studio.

---

## 1. Descripción del proyecto

La app tiene dos pantallas:

- **MainActivity ("Registro de Personas")**: un formulario con campos ID, Nombre y Apellido, y cinco botones (Guardar, Actualizar, Buscar, Eliminar, Listar) que ejecutan operaciones CRUD sobre la base de datos.
- **Listado ("Listado de Personas")**: muestra todos los registros guardados en una tabla dinámica generada por código, con un botón para regresar al formulario.

Toda la persistencia de datos se maneja con **SQLite**, la base de datos embebida que trae Android, sin librerías externas.

---

## 2. Tecnologías y herramientas utilizadas

| Herramienta / Tecnología | Uso en el proyecto |
|---|---|
| **Android Studio** | IDE principal para escribir, compilar y depurar la app |
| **Java** | Lenguaje de programación de toda la lógica (Activities, helpers) |
| **XML** | Definición de las pantallas (layouts) en `res/layout` |
| **SQLite** | Base de datos local embebida (`SQLiteOpenHelper`, `SQLiteDatabase`) |
| **Gradle** | Sistema de build del proyecto (`build.gradle`, dependencias) |
| **AVD (Android Virtual Device)** | Emulador "Pixel 8" usado para probar la app |
| **AndroidX / AppCompat** | Librerías de compatibilidad (`AppCompatActivity`, `EdgeToEdge`) |
| **Logcat / Problems panel** | Herramientas de Android Studio para ver errores de compilación y en tiempo de ejecución |

---

## 3. Estructura del proyecto

```
app/src/main/java/com/example/myapplication/
├── MainActivity.java        // Pantalla de formulario + botones CRUD
├── Listado.java              // Pantalla que muestra todos los registros
├── DynamicTable.java         // Clase de apoyo que arma una TableLayout por código
├── FeedReaderContract.java   // "Contrato" de la tabla: nombre y columnas
└── FeedReaderDbHelper.java   // SQLiteOpenHelper: crea/actualiza la base de datos

app/src/main/res/layout/
├── activity_main.xml         // Layout del formulario
└── activity_listado.xml      // Layout del listado

app/src/main/AndroidManifest.xml  // Declara las dos Activities
```

### ¿Qué hace cada clase?

- **`FeedReaderContract`**: define el nombre de la tabla (`persona`) y de las columnas (`nombre`, `apellido`), más el `_id` que da Android automáticamente. Es solo un lugar centralizado con constantes, para no escribir los nombres "a mano" en cada consulta.
- **`FeedReaderDbHelper`**: extiende `SQLiteOpenHelper`. Se encarga de crear la tabla la primera vez (`onCreate`) y de recrearla cuando cambia la versión de la base de datos (`onUpgrade`).
- **`MainActivity`**: tiene los `EditText` del formulario y un método por cada botón (`guardar`, `Buscar`, `Actualizar`, `Eliminar`, `Listar`), cada uno hace una operación distinta contra la base (`insert`, `query`, `update`, `delete`).
- **`Listado`**: al abrirse, consulta todos los registros de la tabla y usa `DynamicTable` para dibujarlos en pantalla.
- **`DynamicTable`**: clase propia (no es de Android) que arma filas y celdas de una `TableLayout` por código, en vez de tenerlas fijas en el XML.

---

## 4. Funcionalidades implementadas (CRUD)

| Botón | Operación SQL | Método Java |
|---|---|---|
| Guardar | `INSERT` | `db.insert(...)` |
| Buscar | `SELECT ... WHERE _id = ?` | `db.query(...)` |
| Actualizar | `UPDATE ... WHERE _id = ?` | `db.update(...)` |
| Eliminar | `DELETE ... WHERE _id = ?` | `db.delete(...)` |
| Listar | `SELECT *` (todos los registros) | `db.query(...)` desde `Listado` |

**Dato importante para probarlo bien:** el campo "ID" del formulario **no se usa al guardar** (el `_id` lo genera SQLite automáticamente, empezando en 1). Por eso, para *buscar*, *actualizar* o *eliminar* un registro hay que escribir el ID real que SQLite le asignó (se muestra en el `Toast` después de Guardar, o se puede ver completo en la pantalla "Listar").

---

## 5. Problemas que encontré y cómo los resolví

Esta sección documenta los errores reales que fui encontrando mientras programaba, porque fue donde más aprendí.

### 5.1 `Cannot resolve symbol 'main'`
El código buscaba `findViewById(R.id.main)` para aplicar el padding de los insets del sistema, pero el layout raíz (`ScrollView`) no tenía ese `id` declarado. **Solución:** agregar `android:id="@+id/main"` al `ScrollView`.

### 5.2 `Cannot resolve symbol 'getWritableDatabase'`
Estaba escrito `new dbHelper.getWritableDatabase()`. El `new` sobraba: `getWritableDatabase()` es un **método**, no un constructor. **Solución:** quitar el `new`.

### 5.3 `Cannot resolve symbol 'Listado'`
El botón "Listar" abría una Activity (`Listado.class`) que todavía no existía en el proyecto. **Solución:** crear la clase `Listado.java`, su layout `activity_listado.xml`, y registrarla en `AndroidManifest.xml` (toda Activity nueva debe declararse ahí o Android no sabe que existe).

### 5.4 Los IDs siempre se guardaban como 0
Al revisar el listado, todos los registros mostraban `_id = 0`. La causa fue un **typo en el SQL**: `FeedReaderContract` creaba la columna como `"INTENGER PRIMARY KEY"` en vez de `"INTEGER PRIMARY KEY"`. En SQLite, para que una columna `PRIMARY KEY` se autoincremente y sea alias del `rowid`, el tipo debe escribirse **exactamente** `INTEGER` — cualquier otra variante rompe ese comportamiento especial y la columna se queda en `NULL` (que al leerla como número se ve como `0`).

**Lección clave:** una vez que la base de datos ya se creó (aunque sea con un error), cambiar solo el código Java no arregla los datos existentes. Hay que subir el número de `DATABASE_VERSION` en `FeedReaderDbHelper` para que Android dispare `onUpgrade()` y recree la tabla con el esquema corregido (o desinstalar la app del emulador).

### 5.5 `db.query()` con la cantidad incorrecta de argumentos
En `Listado.TraerDatos()` se llamó a `db.query(...)` con **8 argumentos** en vez de 7. `SQLiteDatabase` tiene una versión de `query()` con un parámetro extra llamado `limit` al final; sin darme cuenta, mi variable de orden (`sortOrder`) terminó en la posición de `limit` en lugar de `orderBy`, generando SQL inválido (`LIMIT apellido ASC`). No era un error de compilación (los tipos coincidían), pero sí un bug real de lógica. **Lección:** contar bien los argumentos de métodos sobrecargados (*overloaded methods*), porque Java no siempre avisa si accidentalmente se llama a la versión "equivocada" de un método.

### 5.6 Botones que no hacían nada
Los métodos `guardar`, `Buscar`, `Actualizar`, `Eliminar`, `Listar` y `Regresar` existían en el código pero **ningún botón los llamaba**, porque faltaba el atributo `android:onClick="nombreDelMetodo"` en el XML. Android Studio incluso lo advertía como "Method is never used". **Solución:** agregar `android:onClick` a cada `Button` en el XML, apuntando al método correspondiente (con la firma `public void metodo(View v)`).

### 5.7 El botón "Regresar" desaparecía
En `activity_listado.xml`, el `TableLayout` tenía `android:layout_height="match_parent"`. Dentro de un `LinearLayout` vertical, eso hace que la tabla reclame **todo** el espacio restante de la pantalla, sin dejarle lugar al botón de abajo. **Solución:** usar el patrón `layout_height="0dp"` + `layout_weight="1"`, que reparte el espacio disponible dejando un hueco fijo para los elementos siguientes (en este caso, el botón).

### 5.8 El contenido pegado a los bordes de la pantalla
El editor visual (Design view) mostraba el formulario con márgenes, pero en el emulador se veía todo pegado a los bordes. La causa: el listener de "edge-to-edge" (`ViewCompat.setOnApplyWindowInsetsListener`) hacía `v.setPadding(...)` sobre el mismo `ScrollView` que tenía el `android:padding="16dp"` en el XML — y ese `setPadding` en Java **reemplaza** por completo el padding declarado en XML con los insets del sistema (que en los costados suelen ser 0). El editor visual no ejecuta el código Java, por eso mostraba algo distinto a lo real. **Solución:** mover el `padding="16dp"` del `ScrollView` (que recibe los insets del sistema) al `LinearLayout` interno (que el código Java nunca toca), separando "espacio para no chocar con la barra de estado/navegación" de "espacio visual del diseño".

---

## 6. Lo que aprendí siendo principiante en Android Studio

- **Estructura de un proyecto Android**: separación entre `java/` (lógica), `res/layout/` (interfaces XML), `res/values/` (strings, colores, temas) y `AndroidManifest.xml` (registro de todas las pantallas y permisos de la app).
- **El archivo `R`**: Android genera automáticamente una clase `R` con referencias a cada recurso (`R.id`, `R.layout`, etc.). Si un `id` no existe en el XML, `R` no lo tiene, y el código que lo use no compila.
- **`findViewById`**: es el puente entre el XML (diseño) y el Java (lógica); conecta una vista declarada en el layout con una variable en el código.
- **Ciclo de vida de una Activity**: `onCreate()` es el primer método que se ejecuta al abrir una pantalla; ahí se debe llamar `setContentView()` antes de usar `findViewById`.
- **Conectar botones a código**: se puede hacer con `android:onClick` en el XML (más simple, usado en este proyecto) o con `setOnClickListener()` en Java (más flexible, usado en apps más grandes).
- **SQLite en Android**:
  - `SQLiteOpenHelper` administra la creación y las versiones de la base de datos.
  - Cambiar el esquema (una tabla) requiere subir `DATABASE_VERSION`, porque Android no vuelve a crear la base solo porque el código cambió.
  - Los métodos `insert`, `query`, `update` y `delete` de `SQLiteDatabase` reciben el nombre de tabla, columnas y condiciones como parámetros, en vez de escribir el SQL completo a mano (esto evita errores de sintaxis, pero hay que tener cuidado con el **orden y cantidad de parámetros**, como aprendí con el bug del `LIMIT`).
  - Un `Cursor` siempre debe cerrarse (`cursor.close()`) después de usarlo, igual que la base (`db.close()`), para no dejar recursos abiertos.
- **Layouts y diseño responsivo**:
  - `match_parent` vs `wrap_content` vs `0dp` + `layout_weight`: cada uno cambia cómo se reparte el espacio entre elementos, y usarlos mal puede hacer que un elemento "empuje" a otro fuera de la pantalla.
  - La vista previa del editor visual **no ejecuta el código Java**, así que puede verse distinta al resultado real si hay lógica que modifica las vistas en tiempo de ejecución (como el manejo de insets para edge-to-edge).
- **Diferencia entre errores de compilación y errores de lógica**: un programa puede compilar perfecto (sin errores rojos) y aun así tener bugs, como el de `db.query()` con argumentos de más, o el de los botones sin `onClick`. Compilar no significa que el programa haga lo que uno espera.
- **Depuración metódica**: leer el mensaje de error exacto, ubicar la línea, y entender *por qué* el compilador o la app se queja — en vez de solo copiar una solución sin entenderla — fue clave para resolver cada uno de los problemas de este proyecto.

---

## 7. Cómo ejecutar el proyecto

1. Abrir la carpeta `MyApplication2` en Android Studio.
2. Esperar a que Gradle sincronice las dependencias.
3. Seleccionar un emulador (por ejemplo, Pixel 8) o conectar un dispositivo físico.
4. Presionar **Run ▶** (o Shift+F10).
5. Probar el flujo: Guardar un registro → anotar el ID que aparece en el mensaje → Buscarlo por ese ID → Actualizarlo o Eliminarlo → usar Listar para ver todos los registros.

---

## 8. Posibles mejoras futuras

- Mostrar un mensaje cuando `Buscar` no encuentra ningún registro (actualmente no da ningún aviso).
- Usar `RecyclerView` en vez de una tabla armada a mano, para listas más grandes.
- Envolver el listado en un `ScrollView` para poder desplazarse si hay muchos registros.
- Validar que los campos no estén vacíos antes de guardar.
- Migrar la lógica de acceso a datos a una clase `Repository` separada de las Activities, siguiendo buenas prácticas de arquitectura Android.
