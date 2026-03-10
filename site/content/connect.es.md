---
title: "Connect"
description: "Vincula tu teléfono con la aplicación de escritorio Spyglass Connect para consultar el inventario, encontrar cofres, localizar estructuras y explorar un mapa — todo por WiFi local."
subtitle: "Tu mundo de Minecraft, en tu teléfono."
cssClass: "connect-page"
---

> **Software en fase alfa** — Spyglass Connect está en desarrollo activo. Es posible que encuentres errores, funciones incompletas y detalles por pulir.

## Descripción general

Spyglass Connect es una aplicación de escritorio complementaria que lee los archivos de guardado de Minecraft Java Edition y transmite los datos a tu teléfono por WiFi local. Sin servidores en la nube, sin cuentas — todo permanece en tu red.

**[Obtén Spyglass Connect para escritorio](https://github.com/beryndil/Spyglass-Connect)**

## Cómo funciona

1. **Inicia** Spyglass Connect en tu PC (Windows, macOS o Linux)
2. **Escanea** el código QR que aparece en tu PC desde la app Spyglass en tu teléfono
3. **Listo** — tu teléfono se reconecta automáticamente cuando ambos dispositivos estén en la misma WiFi

Bajo el capó: el código QR empareja los dispositivos mediante intercambio de claves ECDH, y luego se comunican a través de un WebSocket cifrado (AES-256-GCM). mDNS gestiona la reconexión automática.
La negociación de versión del protocolo asegura que ambas apps sean compatibles — si alguna está desactualizada, verás un mensaje de error claro.

## Características

### Visor de personaje

Consulta el equipo completo de tu personaje — armadura, objetos en mano, mano secundaria y todas las estadísticas. Toca cualquier objeto para ver su página de detalle completa en Spyglass.

### Visor de inventario

Explora tu inventario completo, ranuras de armadura, mano secundaria y contenido del cofre del End. Cada objeto tiene enlace cruzado a la base de datos de Spyglass.

### Buscador de cofres

Busca cualquier objeto en **todos los contenedores** de tu mundo — cofres, barriles, cajas de shulker, tolvas y más. Los resultados muestran el tipo de contenedor, coordenadas y cantidad de objetos.

### Localizador de estructuras

Encuentra aldeas, templos, monumentos, fortalezas y todas las demás estructuras generadas. Los resultados incluyen coordenadas y distancia desde tu posición actual.

### Mapa aéreo

Un mapa interactivo del terreno que muestra marcadores de estructuras, la posición de tu personaje y características del terreno. Haz zoom y desplázate para explorar tu mundo.

## Requisitos

- [Spyglass Connect](https://github.com/beryndil/Spyglass-Connect) ejecutándose en tu PC
- Ambos dispositivos en la misma red WiFi
- Archivos de guardado de Minecraft Java Edition accesibles en tu PC
- Protocolo v2+ de Spyglass Connect (ambas apps deben soportar la misma versión del protocolo)
