# Conformidad con el contrato de casos de uso

Este documento traza cada requerimiento funcional (RF) y caso de uso (CU) del
documento **Casos de Uso - 1** contra el código de producción de la aplicación.

- Contrato de referencia: [`requerimientos-casos-de-uso.md`](requerimientos-casos-de-uso.md)
- Estado verificado sobre la rama `master`.
- La evidencia se cita por **archivo y símbolo** (clase o método) y no por número de
  línea, para que la referencia no quede obsoleta con el próximo cambio.
- Verificación de compilación: `:app:assembleDebug` y `:app:testDebugUnitTest` con
  `--rerun-tasks` (recompilación completa del árbol de trabajo) en verde:
  78 pruebas, 0 fallos, 0 errores, 0 omitidas, sobre 13 clases de prueba.

## Módulo de autenticación, usuarios y roles

| Ítem | Estado | Evidencia |
| --- | --- | --- |
| RF-01 Iniciar sesión | Cumple | `LoginActivity.intentarIniciarSesion` filtra los usuarios inactivos (`activo = 1`) antes de llamar a `DatabaseHelper.autenticar`, que solo compara credenciales; `SessionManager.createLoginSession` abre la sesión |
| RF-02 Cerrar sesión | Cumple | `SessionManager.logoutUser` + `MainActivity` (`NEW_TASK` \| `CLEAR_TASK`) |
| RF-27 Administrar usuarios y roles | Cumple | `UsuariosFragment`, `RolesFragment`, `DatabaseHelper.isUsernameExists` / `isRoleNameExists` (`COLLATE NOCASE`) / `insertUsuario` / `updateUsuario` / `deleteUsuario` / `getAllRoles` / `insertRol` / `updateRol` / `deleteRol` |
| CU-01 Iniciar sesión | Cumple | `LoginActivity`; mensajes en `strings.xml` (`error_inactive_user`, `error_credenciales`); foco al campo de usuario y sesión cifrada en `SessionManager` |
| CU-02 Cerrar sesión | Cumple | `MainActivity.mostrarDialogoCerrarSesion` con `AlertDialog` |
| CU-27 Administrar usuarios y roles | Cumple | `UsuariosFragment` con `tilUsername.setError` para el duplicado y listado reactivo (`DatabaseHelper.observarUsuarios`); `RolesFragment` persiste los permisos por rol y la baja está protegida por `DatabaseHelper.deleteRol`; esos permisos son los que autorizan el acceso (`Permisos.tiene`, `AccesoModulo.permitido`) |

### Edición, baja y permisos (RF-27)

**Los permisos del rol autorizan el acceso.** `ROL.permisos` no se limita a guardarse y
volver a mostrarse: `Permisos.tiene` compara por token completo y `DatabaseHelper.getPermisosDeRol`
lee la columna, de modo que el juego de permisos de un rol decide qué módulos abre.
`SessionManager.tienePermiso` consulta la base en cada apertura, así que un cambio de
permisos rige sin volver a iniciar sesión.

- **Panel.** `MainActivity.mostrarSegunPermiso` aplica la visibilidad de los cuatro
  accesos: usuarios y roles con `USUARIOS`, categorías con `CATEGORIAS`, productos con
  `PRODUCTOS` y proveedores con `PROVEEDORES`.
- **Puerta de cada módulo.** Ocultar el botón no alcanza, porque la actividad puede abrirse
  con un intent directo o quedar en una tarea restaurada: `AccesoModulo.permitido` vuelve
  a validar el permiso al crear `CategoriasActivity`, `ProductosActivity` y
  `ProveedoresActivity`, y `UserRoleAdminActivity.rolRealPuedeAdministrarUsuarios` hace lo
  propio con `USUARIOS` sobre la fila real de `USUARIO`, con el rol reconsultado en la
  base y no tomado de la copia de la sesión.
- **Efecto observable.** Un rol con el permiso `USUARIOS` marcado entra a la administración
  aunque no se llame `Administrador`, y un rol como `Vendedor` (`PRODUCTOS,CATEGORIAS`) no
  ve ni abre proveedores ni movimientos. Antes el nombre del rol era la única llave.

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
| RF-03 Registrar productos | Cumple | `ProductoFormActivity` expone el stock inicial, lo valida (`≥ 0`) y lo asienta como entrada auditada; `ProductoDao.insertProducto` (SKU único) y `ProductoDao.existeSkuEnOtroProducto` |
| RF-04 & RF-05 Consultar y buscar | Cumple | `ProductosActivity` observa `DatabaseHelper.observarProductos`; consulta por nombre o SKU en un hilo de trabajo, no en el hilo principal |
| RF-07 Modificar productos | Cumple | `ProductoFormActivity.guardarEdicion` + `ProductoDao.updateProducto` |
| RF-08 Deshabilitar productos | Cumple | `ProductoDao.updateProductoEstado` (baja lógica: `activo = 0`, sin borrar la fila) |
| Entradas y salidas de stock | Cumple | `ProductoFormActivity` + `dialog_movimiento_stock.xml` + `ProductoDao.actualizarStock`, que rechaza la salida sin stock suficiente |

### SKU automático y proveedor opcional

- **SKU.** Cuando el formulario se guarda con el campo SKU vacío, `ProductoFormActivity.guardar`
  genera un código `SKU-<epoch>-<aleatorio>`, lo escribe en el campo para que quede a la vista
  y verifica la unicidad contra todos los productos, incluidos los deshabilitados (el índice
  `UNIQUE` de `sku_codigo` es global). `guardarAlta` reintenta una vez si la inserción devuelve
  `-1` por colisión imprevista.
- **Stock inicial.** El alta expone `etStockInicial`; el campo vacío vale cero y un valor
  negativo se rechaza con `msg_valores_invalidos` (el campo es `inputType="number"`, así que
  no admite texto; `parseEntero` devuelve `0` ante algo que no sea número). El producto se inserta en
  cero y el stock inicial se asienta como una entrada auditada (`ProductoDao.actualizarStock`
  con `TipoMovimiento.ENTRADA`), de modo que el historial muestra de dónde salieron las
  existencias en vez de aparecer como un número sin origen. La edición no expone el campo:
  el stock se cambia con movimientos (RF-07).
- **Proveedor opcional.** El formulario envía `0` cuando el usuario elige *Sin proveedor*. Con
  las claves foráneas activas, `0` no existe en `PROVEEDOR` y el alta fallaba siempre con
  `FOREIGN KEY constraint failed`; `ProductoDao.insertProducto` y `ProductoDao.updateProducto`
  ahora guardan `NULL` en `PRODUCTO.id_proveedor`, que es lo que dice el esquema.

## Esquema de datos

El esquema se mantiene en la **versión 5**. La migración de `onUpgrade` es incremental e
idempotente: cada paso comprueba con `PRAGMA table_info` si la columna ya está
(`DatabaseHelper.columnaExisteEnTabla` / `agregarColumnaSiFalta`), de modo que un `ALTER TABLE`
interrumpido a medias se reintenta en el siguiente arranque en lugar de dejarse caer.

La v5 no solo agrega la columna: además **rellena** los roles que ya existían
(`DatabaseHelper.rellenarPermisosDeRolesExistentes`). Como a partir de esta versión la columna
es lo que autoriza, dejarla en `NULL` habría dejado sin acceso a ningún módulo a cualquier
instalación que se actualizara, administrador incluido. El relleno replica el modelo de acceso
anterior —`Administrador` recibe los cinco módulos y el resto los de inventario—, solo toca las
filas vacías, y la ruta v4→v5 está simulada contra SQLite en `simular_migracion_v5.py`.

| Versión | Cambio |
| --- | --- |
| v4 | `MOVIMIENTO.id_proveedor` para asociar la entrada a su proveedor |
| v5 | `ROL.permisos` para los módulos habilitados por rol, con relleno de los roles existentes |

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
| `feat(permisos)` | Los permisos del rol autorizan el acceso a los módulos |
| `feat(productos)` | Stock inicial en el alta, validado y auditado como entrada |

## Límites conocidos

- **Pruebas:** las 78 pruebas unitarias del módulo verifican estructura (presencia de
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
  `PRODUCTO` reciben claves foráneas desde `PRODUCTO` y `MOVIMIENTO`. Las dos reglas se
  validan en los formularios, que es lo que exige el contrato, y no en la base: el contacto
  en el alta y la edición de proveedores, y el stock porque siempre entra por movimientos
  que ya rechazan la salida sin existencias.
- **Contraseñas:** `PasswordUtils` guarda los hashes con **PBKDF2-HMAC-SHA256** (sal
  aleatoria de 16 bytes por usuario, 120.000 iteraciones, 256 bits, formato
  autodescriptivo `pbkdf2$iteraciones$sal$hash`). Los hashes heredados (SHA-256 sin sal)
  siguen verificándose para no romper las bases ya instaladas, y se reescriben con el
  formato nuevo de forma transparente en el primer inicio de sesión correcto. El acceso
  a la administración vuelve a comprobar el rol y sus permisos en la base de datos en
  lugar de confiar en la preferencia de sesión.
