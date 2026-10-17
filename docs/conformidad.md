# Conformidad con el contrato de casos de uso

Este documento traza cada requerimiento funcional (RF) y caso de uso (CU) del
documento **Casos de Uso - 1** contra el código de producción de la aplicación.

- Contrato de referencia: [`requerimientos-casos-de-uso.md`](requerimientos-casos-de-uso.md)
- Estado verificado sobre la rama `master`.
- La evidencia se cita por **archivo y símbolo** (clase o método) y no por número de
  línea, para que la referencia no quede obsoleta con el próximo cambio.
- Verificación de compilación: `:app:assembleDebug` y `:app:testDebugUnitTest` con
  `--rerun-tasks` (recompilación completa del árbol de trabajo) en verde:
  63 pruebas, 0 fallos, 0 errores, 0 omitidas, sobre 11 clases de prueba.

## Módulo de autenticación, usuarios y roles

| Ítem | Estado | Evidencia |
| --- | --- | --- |
| RF-01 Iniciar sesión | Cumple | `LoginActivity` → `DatabaseHelper.autenticar` (solo `activo = 1`) y `SessionManager.createLoginSession` |
| RF-02 Cerrar sesión | Cumple | `SessionManager.logout` + `MainActivity` (`NEW_TASK` \| `CLEAR_TASK`) |
| RF-27 Administrar usuarios y roles | Cumple | `UsuariosFragment`, `RolesFragment`, `DatabaseHelper.isUsernameExists` / `isRoleNameExists` (`COLLATE NOCASE`) / `insertUsuario` / `updateUsuario` / `deleteUsuario` / `getAllRoles` / `insertRol` / `updateRol` / `deleteRol` |
| CU-01 Iniciar sesión | Cumple | `LoginActivity`; mensajes en `strings.xml` (`error_inactive_user`, `error_credenciales`); foco al campo de usuario y sesión cifrada en `SessionManager` |
| CU-02 Cerrar sesión | Cumple | `MainActivity.confirmarCierreSesion` con `AlertDialog` |
| CU-27 Administrar usuarios y roles | Cumple | `UsuariosFragment` con `tilUsername.setError` para el duplicado y listado reactivo (`DatabaseHelper.observarUsuarios`); `RolesFragment` con permisos por rol y baja protegida por `DatabaseHelper.deleteRol` |

### Edición, baja y permisos (RF-27)

- **Roles.** `Rol.permisos` persiste los módulos habilitados. `dialog_add_rol.xml` los
  expone como casillas y `RolesFragment` reutiliza el mismo diálogo para el alta y para
  la edición; `item_rol.xml` agrega las acciones de editar y eliminar.
- **Usuarios.** `UsuariosFragment` abre el diálogo de edición con los datos de la fila y
  deja la contraseña vacía para conservar la actual: `DatabaseHelper.updateUsuario` solo
  reescribe el hash cuando llega una contraseña nueva. `item_usuario.xml` agrega las
  acciones de editar y eliminar.
- **Integridad.** La baja de un rol con usuarios asignados se rechaza en la capa de
  datos y no solo en la interfaz (`DatabaseHelper.deleteRol` consulta `USUARIO` antes de
  borrar), para que la clave foránea de `USUARIO` no quede apuntando a un rol
  inexistente.

## Módulo de categorías y proveedores

| Ítem | Estado | Evidencia |
| --- | --- | --- |
| RF-09 Registrar categorías | Cumple | `CategoriasActivity` → `DatabaseHelper.insertCategoria` (nombre único, `activo = 1` por defecto) |
| CU-09 Registrar categoría | Cumple | `CategoriasActivity`; mensaje `msg_categoria_duplicada` |
| RF-10 Consultar categorías activas | Cumple | `DatabaseHelper.getCategoriasActivas` / `observarCategoriasActivas` (LiveData, `ORDER BY nombre_categoria ASC`) |
| CU-10 Consultar categorías activas | Cumple | `CategoriasActivity` observa `observarCategoriasActivas` |
| Proveedores | Cumple | `ProveedoresActivity`, `ProveedorFormActivity`, `ProveedorDao`; razón social, RUT único y **número de contacto obligatorios**; el proveedor queda asociado al movimiento de entrada (`MOVIMIENTO.id_proveedor`) y se muestra en el historial |

## Módulo de inventario y stock

| Ítem | Estado | Evidencia |
| --- | --- | --- |
| RF-03 Registrar productos | Cumple | `ProductoFormActivity` + `ProductoDao.insertProducto` (SKU único, stock inicial ≥ 0) y `ProductoDao.existeSkuEnOtroProducto` |
| RF-04 & RF-05 Consultar y buscar | Cumple | `ProductosActivity` observa `DatabaseHelper.observarProductos`; consulta por nombre o SKU en un hilo de trabajo, no en el hilo principal |
| RF-07 Modificar productos | Cumple | `ProductoFormActivity.guardarEdicion` + `ProductoDao.updateProducto` |
| RF-08 Deshabilitar productos | Cumple | `ProductoDao.deshabilitarProducto` (baja lógica: `activo = 0`, sin borrar la fila) |
| Entradas y salidas de stock | Cumple | `ProductoFormActivity` + `dialog_movimiento_stock.xml` + `ProductoDao.registrarMovimiento`, que rechaza la salida sin stock suficiente |

### SKU automático y proveedor opcional

- **SKU.** Cuando el formulario se guarda con el campo SKU vacío, `ProductoFormActivity.guardar`
  genera un código `SKU-<epoch>-<aleatorio>`, lo escribe en el campo para que quede a la vista
  y verifica la unicidad contra todos los productos, incluidos los deshabilitados (el índice
  `UNIQUE` de `sku_codigo` es global). `guardarAlta` reintenta una vez si la inserción devuelve
  `-1` por colisión imprevista.
- **Proveedor opcional.** El formulario envía `0` cuando el usuario elige *Sin proveedor*. Con
  las claves foráneas activas, `0` no existe en `PROVEEDOR` y el alta fallaba siempre con
  `FOREIGN KEY constraint failed`; `ProductoDao.insertProducto` y `ProductoDao.updateProducto`
  ahora guardan `NULL` en `PRODUCTO.id_proveedor`, que es lo que dice el esquema.

## Esquema de datos

El esquema se mantiene en la **versión 5**. La migración de `onUpgrade` es incremental e
idempotente: cada paso comprueba con `PRAGMA table_info` si la columna ya está
(`DatabaseHelper.columnaExisteEnTabla` / `agregarColumnaSiFalta`), de modo que un `ALTER TABLE`
interrumpido a medias se reintenta en el siguiente arranque en lugar de dejarse caer.

| Versión | Cambio |
| --- | --- |
| v4 | `MOVIMIENTO.id_proveedor` para asociar la entrada a su proveedor |
| v5 | `ROL.permisos` para los módulos habilitados por rol |

El rol semilla `Administrador` se inserta con el juego completo de permisos
(`USUARIOS,PRODUCTOS,CATEGORIAS,PROVEEDORES,MOVIMIENTOS`).

## Correcciones aplicadas

| Commit | Corrección |
| --- | --- |
| `refactor(categorias)` | Unifica el acceso a categorías: elimina la vía de lectura sin uso |
| `fix(productos)` | Evita el cierre inesperado al reasignar el SKU de un producto deshabilitado |
| `fix(i18n)` | Deja en recursos los textos exactos que exige el contrato |
| `feat(usuarios)` | Error de username duplicado en el campo y listado reactivo |
| `feat(seguridad)` | Sesión en `EncryptedSharedPreferences` y respaldo deshabilitado |
| `feat(movimientos)` | El proveedor de la entrada se muestra en el historial |
| `fix(proveedores)` | Número de contacto obligatorio en el alta y la edición |
| `feat(productos)` | Búsqueda reactiva fuera del hilo principal |
| `fix(seguridad)` | Contraseñas con PBKDF2, sal e iteración, con migración transparente del hash heredado |
| `fix(seguridad)` | El acceso a la administración reconsulta el rol en la base de datos |
| `perf(ui)` | Consultas de lista de proveedores y del formulario fuera del hilo principal |
| `chore(limpieza)` | Recursos e imports sin uso y un chequeo inalcanzable |
| `fix(db)` | Pasos de migración idempotentes que registran los fallos en vez de silenciarlos |
| `feat(db)` | Esquema a la versión 5: `ROL.permisos` y la persistencia de edición y baja de roles y usuarios |
| `feat(roles)` | Permisos por rol y acciones de editar y eliminar en el listado |
| `feat(usuarios)` | Edición y baja de usuarios desde la administración |
| `fix(productos)` | `NULL` en `id_proveedor` cuando el producto no tiene proveedor |
| `feat(productos)` | SKU generado automáticamente cuando el usuario no lo indica |
| `feat(icono)` | Rediseño del icono del lanzador y variante monocroma |

## Límites conocidos

- **Pruebas:** las 63 pruebas unitarias del módulo verifican estructura (presencia de
  métodos, de columnas del esquema y de cadenas en los recursos), no comportamiento en
  ejecución. No hay pruebas instrumentadas ni Robolectric, por lo que las correcciones de
  base de datos están verificadas por compilación y revisión del código, no por ejecución
  contra una base real en un dispositivo.
- **Hilo principal:** la administración (alta, edición y baja de usuarios y roles) y el
  formulario de producto escriben en la base de datos desde el hilo de la interfaz. Es
  deuda previa que estos cambios extienden: el alta de un producto comprueba el SKU de
  forma síncrona antes de insertar y el cálculo del hash PBKDF2 (120.000 iteraciones) al
  guardar un usuario se hace en el mismo hilo. La base es local y pequeña, así que el
  efecto es una pausa breve, no un cierre; el proyecto ya había sacado del hilo principal
  las consultas de lista.
- **Baja de usuarios:** el diálogo de eliminación pide confirmación, pero no bloquea
  eliminar el último administrador ni el usuario que tiene la sesión abierta. El contrato
  no lo exige; queda como endurecimiento pendiente.
- **Textos:** los diálogos nuevos de la administración llevan sus cadenas literales en el
  código en vez de en `strings.xml`, a diferencia del resto de la aplicación.
- **Esquema:** reforzar `NOT NULL` en `PROVEEDOR.telefono` o agregar
  `CHECK (stock_actual >= 0)` en SQLite obliga a reconstruir la tabla, y `PROVEEDOR` y
  `PRODUCTO` reciben claves foráneas desde `PRODUCTO` y `MOVIMIENTO`. La obligatoriedad
  del contacto se valida en el formulario, que es lo que exige el contrato.
- **Contraseñas:** `PasswordUtils` guarda los hashes con **PBKDF2-HMAC-SHA256** (sal
  aleatoria de 16 bytes por usuario, 120.000 iteraciones, 256 bits, formato
  autodescriptivo `pbkdf2$iteraciones$sal$hash`). Los hashes heredados (SHA-256 sin sal)
  siguen verificándose para no romper las bases ya instaladas, y se reescriben con el
  formato nuevo de forma transparente en el primer inicio de sesión correcto. El acceso
  a la administración vuelve a comprobar el rol en la base de datos en lugar de confiar
  en la preferencia de sesión.
