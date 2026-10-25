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
