# Conformidad con el contrato de casos de uso

Este documento traza cada requerimiento funcional (RF) y caso de uso (CU) del
documento **Casos de Uso - 1** contra el código de producción de la aplicación.

- Contrato de referencia: [`requerimientos-casos-de-uso.md`](requerimientos-casos-de-uso.md)
- Estado verificado sobre la rama `feature/casos-de-uso-completo`.
- La evidencia se cita por **archivo y símbolo** (clase o método) y no por número de
  línea, para que la referencia no quede obsoleta con el próximo cambio.
- Verificación de compilación: `:app:assembleDebug` y `:app:testDebugUnitTest`
  en verde (57 pruebas, 0 fallos).

## Módulo de autenticación, usuarios y roles

| Ítem | Estado | Evidencia |
| --- | --- | --- |
| RF-01 Iniciar sesión | Cumple | `LoginActivity` → `DatabaseHelper.autenticar` (solo `activo = 1`) y `SessionManager.createLoginSession` |
| RF-02 Cerrar sesión | Cumple | `SessionManager.logout` + `MainActivity` (`NEW_TASK` \| `CLEAR_TASK`) |
| RF-27 Administrar usuarios y roles | Cumple | `UsuariosFragment`, `RolesFragment`, `DatabaseHelper.isUsernameExists` / `getAllRoles` / `insertUsuario` |
| CU-01 Iniciar sesión | Cumple | `LoginActivity`; mensajes en `strings.xml` (`error_inactive_user`, `error_credenciales`); foco al campo de usuario y sesión cifrada en `SessionManager` |
| CU-02 Cerrar sesión | Cumple | `MainActivity.confirmarCierreSesion` con `AlertDialog` |
| CU-27 Administrar usuarios y roles | Cumple | `UsuariosFragment` con `tilUsername.setError` para el duplicado y listado reactivo (`DatabaseHelper.observarUsuarios`) |

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

## Límites conocidos

- **Pruebas:** las 57 pruebas unitarias del módulo verifican estructura (presencia de
  métodos y de cadenas en los recursos), no comportamiento en ejecución. No hay
  pruebas instrumentadas ni Robolectric, por lo que las correcciones de base de datos
  están verificadas por compilación y revisión del código, no por ejecución contra una
  base real en un dispositivo.
- **Esquema:** se mantiene en la versión 4. Reforzar `NOT NULL` en `PROVEEDOR.telefono`
  o agregar `CHECK (stock_actual >= 0)` en SQLite obliga a reconstruir la tabla, y
  `PROVEEDOR` y `PRODUCTO` reciben claves foráneas desde `PRODUCTO` y `MOVIMIENTO`.
  La obligatoriedad del contacto se valida en el formulario, que es lo que exige el
  contrato.
- **Contraseñas:** `PasswordUtils` usa SHA-256 sin sal, con un respaldo que devuelve la
  contraseña en claro. Es una decisión heredada del proyecto base; corregirlo cambia los
  hashes almacenados y requiere migrar las credenciales existentes, por lo que queda
  fuera de esta entrega.
