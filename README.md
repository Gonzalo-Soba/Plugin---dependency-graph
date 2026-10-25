# TaskGraph

Plugin de Paper para organizar las tareas comunales de un servidor de Minecraft. Las tareas forman un grafo de dependencias que se muestra en la pantalla de logros (advancements) vanilla, así que los jugadores no necesitan mods en el cliente.

> Estado: en desarrollo. Por ahora el plugin solo se carga en el servidor; los comandos llegan en próximas versiones.

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

## Comandos

Todavía no hay comandos disponibles.
