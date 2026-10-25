# TaskGraph

Plugin de Paper para organizar las tareas comunales de un servidor de Minecraft. Las tareas forman un grafo de dependencias que se muestra en la pantalla de logros (advancements) vanilla, así que los jugadores no necesitan mods en el cliente.

> Estado: MVP. Todos los jugadores pueden usar todos los comandos (todavía no hay permisos finos).

## Cómo se ve

- Cada tarea es un logro en la pestaña "Tareas del servidor".
- Las tareas sin dependencias cuelgan del logro raíz. La primera dependencia de cada tarea es la línea que la une con su padre; las demás aparecen en la descripción como "También requiere: ...".
- Las columnas indican la profundidad: todo lo que una tarea requiere queda a su izquierda.
- El progreso es comunal: una tarea resuelta aparece completada para todos los jugadores, incluidos los que entran después.

## Requisitos

- Servidor [Paper](https://papermc.io/) para Minecraft Java 26.3
- Java 25
- Plugin [UltimateAdvancementAPI](https://modrinth.com/plugin/ultimateadvancementapi) **3.0.0-beta-4** instalado en el servidor

## Compilar

```bash
./gradlew build
```

En Windows: `gradlew.bat build`. El jar queda en `build/libs/`.

## Instalar en el servidor

1. Descargar `UltimateAdvancementAPI-Plugin-3.0.0-beta-4.jar` desde Modrinth y copiarlo a la carpeta `plugins/` del servidor.
2. Copiar el jar de `build/libs/` a la misma carpeta `plugins/`.
3. Reiniciar el servidor.

## Prueba local

> **Solo para pruebas locales.** El servidor de prueba corre con `online-mode=false` (cualquiera puede entrar con cualquier nombre, sin verificar la cuenta) y escuchando solo en `127.0.0.1`. No uses esta configuración en un servidor público.

El proyecto trae un servidor de prueba con [run-paper](https://github.com/jpenilla/run-task). La tarea `runServer`:
- descarga Paper 26.3 build 222, el mismo build contra el que compila el plugin;
- descarga UltimateAdvancementAPI 3.0.0-beta-4 desde Modrinth;
- compila el plugin y lo instala;
- antes de arrancar, copia `dev-server/server.properties` a `run/`. Solo copia lo que falte en `run/`, así que tus cambios locales se conservan. Para volver a la configuración original, borrá `run/server.properties`.

El servidor vive en `run/`, que está en `.gitignore`.

### Levantar el servidor

1. `./gradlew runServer` (en Windows: `gradlew.bat runServer`).
2. La primera vez el servidor se detiene y pide aceptar la EULA de Minecraft. Leela en https://aka.ms/MinecraftEULA y, si estás de acuerdo, cambiá `eula=false` por `eula=true` en `run/eula.txt`. Después volvé a correr `./gradlew runServer`.
3. Entrá desde el cliente a `localhost`. Para probar con dos jugadores hacen falta dos clientes con **nombres distintos**: si usan el mismo nombre, el segundo echa al primero.
4. Para apagarlo, escribí `stop` en la consola.

El mundo de prueba es plano, en modo creativo y pacífico. Así es fácil agarrar cualquier ítem para usarlo de ícono.

### Checklist de prueba de humo

Jugadores: **A** y **B**.

- [ ] **La pestaña se ve y es legible.** A crea `madera`, `piedra`, `mesa` (depende de `madera`), `horno` (depende de `piedra` y después de `madera`) y `pico` (depende de `mesa` y `piedra`). La pestaña "Tareas del servidor" muestra el raíz a la izquierda, cada tarea a la derecha de sus dependencias, y ningún ícono encima de otro.
- [ ] **Tildes y colores.** Crear una tarea con título y descripción con tildes y ñ, por ejemplo `/tarea crear jardin Jardín de la ñandú` y `/tarea desc jardin Regar todos los días`. En la pestaña se ven bien, con el título en blanco, la descripción en gris, "Asignada a" en celeste y "También requiere" en amarillo.
- [ ] **Sin toasts ni anuncios al entrar o reconstruir.** Con tareas ya resueltas, B sale y vuelve a entrar: no aparece ningún toast ni mensaje de logro en el chat. Lo mismo cuando A edita cualquier tarea (`desc`, `asignar`) y la pestaña se reconstruye.
- [ ] **`/tarea resolver` anuncia una sola vez.** A resuelve una tarea: A y B ven un único mensaje "✔ A resolvió la tarea ...", sin el anuncio de logro vanilla, y la tarea aparece completada para los dos.
- [ ] **Revocación con jugador offline.** A resuelve `jardin` (sin dependencias) y B la ve completada. B sale. A borra `jardin` con `/tarea borrar jardin` y la vuelve a crear con `/tarea crear jardin Jardín`. B entra: `jardin` aparece **pendiente**, no completada.

Si algo falla, guardá `run/logs/latest.log`.

## Datos

Las tareas se guardan en `plugins/TaskGraph/tasks.json`. Ese archivo es la fuente de verdad: el progreso que ve cada jugador en la pantalla de logros se sincroniza contra él.

Si el archivo está dañado o tiene datos inválidos (por ejemplo, un ciclo de dependencias), el plugin se desactiva al arrancar y deja el archivo intacto. Revisar el log del servidor, corregir el archivo y reiniciar.

## Comandos

Escribir `/tarea` sin argumentos muestra la ayuda. Los ids se autocompletan con Tab.

| Comando | Qué hace |
|---|---|
| `/tarea crear <id> <título>` | Crea una tarea. El ícono es el ítem que tenés en la mano (papel si está vacía). El id usa de 1 a 32 caracteres entre `a-z`, `0-9` y `_`. |
| `/tarea desc <id> <texto>` | Cambia la descripción. |
| `/tarea depende <id> <idDependencia>` | Hace que `<id>` dependa de `<idDependencia>`. Se rechaza si crea un ciclo. La primera dependencia es el padre visual; las siguientes se listan como "También requiere". |
| `/tarea asignar <id>` | Te asigna la tarea (reemplaza al asignado anterior). |
| `/tarea resolver <id>` | Marca la tarea como resuelta para todos. Requiere que todas sus dependencias estén resueltas. |
| `/tarea info <id>` | Muestra estado, asignado, dependencias y qué tareas la necesitan. |
| `/tarea borrar <id>` | Borra la tarea. Se rechaza si otras tareas dependen de ella. |

Cada cambio se guarda en el JSON y regenera la pestaña de logros para todos los jugadores online.
