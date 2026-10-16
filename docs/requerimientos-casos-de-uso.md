Requerimientos Funcionales (RF)

1. Módulo de Autenticación, Usuarios y Roles
RF-01: Iniciar Sesión (Must Have)
Descripción: El sistema debe permitir a los usuarios autenticarse mediante su nombre de usuario (username) y contraseña (password_hash).
Tablas asociadas: USUARIO, ROL.
Validaciones / Criterios: Solo pueden ingresar usuarios activos (activo = 1). El sistema debe verificar la concordancia de credenciales y asociar el rol asignado (id_rol) a la sesión activa.

RF-02: Cerrar Sesión (Should Have)
Descripción: El sistema debe permitir al usuario autenticado cerrar su sesión actual de forma segura, limpiando las preferencias locales o estado de sesión y redirigiendo a la pantalla de Login.
Tablas asociadas: USUARIO.

RF-27: Administración de Usuarios y Roles (Should Have)
Descripción: El sistema debe permitir registrar y listar usuarios y roles (ROL: nombre_rol, descripcion; USUARIO: username, nombre_completo, id_rol, activo).
Tablas asociadas: USUARIO, ROL.
Validaciones / Criterios: El username y nombre_rol deben ser únicos (UK).

2. Módulo de Categorías y Proveedores
RF-09: Registrar Categorías (Should Have)
Descripción: El sistema debe permitir crear categorías para clasificar los productos del inventario.
Tablas asociadas: CATEGORIA.
Validaciones / Criterios: El nombre_categoria debe ser único y obligatorio. Por defecto, el estado debe ser activo = 1.

RF-10: Consultar Categorías (Should Have)
Descripción: El sistema debe permitir listar y seleccionar categorías activas disponibles para asignarlas al crear o modificar productos.
Tablas asociadas: CATEGORIA.

Gestión de Proveedores (Soporte a Compras / Entradas):
Descripción: El sistema debe permitir registrar y asociar proveedores a los movimientos de tipo entrada.
Tablas asociadas: PROVEEDOR.
Validaciones / Criterios: El rut_empresa debe ser único (UK). Requiere razón social y número de contacto.

3. Módulo de Gestión de Productos e Inventario
RF-03: Registrar Productos (Must Have)
Descripción: El sistema debe permitir dar de alta nuevos productos asociándolos a una categoría existente (id_categoria).
Campos: sku_codigo, nombre, descripcion, stock_actual, stock_minimo, activo.
Validaciones / Criterios: El código SKU (sku_codigo) debe ser único. El valor de stock_actual inicial debe ser un número entero mayor o igual a 0 (stock_actual >= 0).

RF-04 & RF-05: Consultar y Buscar Productos (Must Have)
Descripción: El sistema debe listar el catálogo de productos y permitir la búsqueda reactiva (ej. mediante Room/LiveData/Flow) por nombre o código SKU.
Tablas asociadas: PRODUCTO, CATEGORIA.

RF-07: Modificar Productos (Must Have)
Descripción: El sistema debe permitir editar la información básica de un producto: nombre, descripción, stock mínimo y categoría asignada.
Tablas asociadas: PRODUCTO.
RF-08: Deshabilitar Productos (Should Have)
Descripción: El sistema debe permitir la baja lógica de un producto cambiando su estado a activo = 0 para no comprometer la integridad referencial con movimientos pasados.
Tablas asociadas: PRODUCTO.

CASOS DE USOS

1. Mapeo General de Actores
Operador / Bodeguero: Usuario autenticado con permisos operativos para consultar productos, registrar entradas y salidas de bodega.
Administrador de Bodega: Usuario con privilegios de gestión (creación de usuarios, roles, categorías, proveedores y configuración general).
Sistema (Android App): Lógica local del cliente móvil (Room, ViewModels, Validadores, Notificaciones visuales).

2. Módulo de Autenticación, Usuarios y Roles
CU-01: Iniciar Sesión en el Sistema
Requerimiento Asociado: RF-01 (Must Have)
Actor Principal: Usuario (Operador o Administrador).
Precondiciones:
La aplicación Android está instalada e iniciada.
La base de datos SQLite/Room contiene al menos un usuario administrador inicial precargado (Database Seeding).
Flujo Principal:
El usuario ingresa a la pantalla de Login.
El usuario introduce su username y password en los campos correspondientes.
El usuario presiona el botón "Iniciar Sesión".
El sistema consulta mediante Room la entidad USUARIO por username.
El sistema verifica que la contraseña ingresada coincida con el password_hash almacenado.
El sistema valida que el campo activo sea igual a 1.
El sistema obtiene el rol asignado (id_rol) y persiste la sesión localmente (EncryptedSharedPreferences o DataStore).
El sistema redirige al usuario a la pantalla principal (Dashboard / Catálogo de Productos).
Flujos Alternativos:
4a. Usuario no existe o credenciales incorrectas:
El sistema muestra un mensaje de error tipo Snackbar/Toast: "Credenciales inválidas".
El foco regresa al campo de usuario manteniendo limpios los campos de contraseña.
6a. Usuario inactivo (activo == 0):
El sistema muestra mensaje: "Usuario deshabilitado. Contacte al administrador".
Se cancela el inicio de sesión.
Postcondiciones: Sesión de usuario activa en el dispositivo con permisos asociados según su rol (id_rol).

CU-02: Cerrar Sesión
Requerimiento Asociado: RF-02 (Should Have)
Actor Principal: Usuario autenticado.
Precondiciones: Existe una sesión activa en la aplicación.
Flujo Principal:
El usuario presiona el ícono o botón "Cerrar Sesión" desde el menú o barra de navegación.
El sistema solicita confirmación mediante un diálogo emergente (AlertDialog).
El usuario confirma la acción.
El sistema limpia las credenciales, identificadores y estado en sesión del almacenamiento local.
El sistema redirige a la pantalla de Login limpiando la pila de actividades/navegación (Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK).
Postcondiciones: La sesión queda cerrada y ninguna pantalla protegida puede abrirse con el botón Atrás.

CU-27: Administrar Usuarios y Roles
Requerimiento Asociado: RF-27 (Should Have)
Actor Principal: Administrador de Bodega.
Precondiciones: El usuario autenticado posee rol con privilegios de Administrador.
Flujo Principal:
El Administrador ingresa al módulo "Gestión de Usuarios".
El sistema consulta y despliega en un RecyclerView el listado de usuarios con su nombre completo, nombre de usuario y rol asignado (ROL.nombre_rol).
El Administrador presiona el botón flotante (FAB) "Nuevo Usuario".
El sistema presenta el formulario de registro solicitando: username, password, nombre_completo y un selector (Spinner/Dropdown) con los roles activos disponibles (ROL).

El Administrador completa los datos y presiona "Guardar".
El sistema valida que el username no exista previamente en la base de datos (restricción UNIQUE).
El sistema genera el hash de la contraseña (password_hash), asigna activo = 1 y persiste el registro en la tabla USUARIO mediante Room Coroutines.
El sistema notifica: "Usuario registrado exitosamente" y actualiza reactivamente el listado.
Flujos Alternativos:
6a. username duplicado:
El sistema intercepta la excepción de clave única (SQLiteConstraintException) o valida previamente con un DAO countByUsername.
Muestra un error en el campo TextInputLayout: "El nombre de usuario ya se encuentra registrado".
Postcondiciones: Nuevo usuario persistido en base de datos disponible para iniciar sesión.

3. Módulo de Categorías y Proveedores
CU-09: Registrar Categoría de Productos
Requerimiento Asociado: RF-09 (Should Have)
Actor Principal: Administrador / Operador de Bodega.
Precondiciones: Sesión iniciada con acceso al catálogo maestro.
Flujo Principal:
El usuario accede a la sección "Categorías" y pulsa "Nueva Categoría".
El sistema despliega un formulario o diálogo de captura.
El usuario ingresa el nombre de la categoría (nombre_categoria) y una descripción opcional.
El usuario presiona "Registrar".
El sistema valida que el nombre no esté vacío y no exista previamente en la base de datos (UK nombre_categoria).
El sistema inserta el registro en la tabla CATEGORIA con activo = 1 por defecto.
El sistema muestra mensaje de confirmación y actualiza la lista.
Flujos Alternativos:
5a. Nombre de categoría ya existe:
El sistema notifica: "Ya existe una categoría registrada con ese nombre".
No se persiste el registro.
Postcondiciones: Categoría registrada y habilitada para ser vinculada a nuevos productos.

CU-10: Consultar y Seleccionar Categorías
Requerimiento Asociado: RF-10 (Should Have)
Actor Principal: Operador / Administrador.
Precondiciones: Existen categorías registradas en la tabla CATEGORIA.
Flujo Principal:
El usuario ingresa a la lista general de categorías o abre el selector de categoría en el formulario de productos.
El sistema ejecuta una consulta DAO que filtra únicamente categorías activas (WHERE activo = 1 ORDER BY nombre_categoria ASC).
El sistema expone la lista a través de un StateFlow o LiveData hacia la interfaz de usuario.
El usuario visualiza el listado o selecciona una categoría para asociarla.
Postcondiciones: Se obtienen y exponen las categorías activas sin alterar el almacenamiento.
