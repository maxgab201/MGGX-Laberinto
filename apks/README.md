# APKs

Acá quedan los APK firmados, directo en el repositorio: es la forma de bajar
el juego mientras las releases de GitHub no estén disponibles.

Bajate el más nuevo, abrilo en el teléfono y dale que sí al permiso de
instalar de origen desconocido. No hace falta desinstalar el anterior: todos
están firmados con la misma clave (ver `keystore/` en la raíz del repo), así
que uno instala encima del otro.

**Esta rama (`main`) es el desarrollo en curso**: acá vive el APK más
reciente, que puede ser una versión todavía sin cerrar (alpha/beta). Cada
versión ya cerrada tiene su propia rama con el número de versión
(`1.0.0`, `1.1.0`, `1.2.0`, ... `1.5.3`), y ahí adentro está su propio
`apks/` con el APK final de esa versión, más un `CAMBIOS.md` y un
`RELEASE.md` para cuando se publique como release de GitHub.

| Version | Archivo | Firma SHA-256 |
|---|---|---|
| 1.6.0-alpha1 | `MGGX-Laberinto-1.6.0-alpha1.apk` | `A3:72:F9:5E:9E:EC:57:46:A2:66:8C:DA:F3:1C:56:6A:FC:97:39:B0:A4:49:A5:C1:6A:FA:66:86:52:4E:31:EE` |
| 1.5.3 | `MGGX-Laberinto-1.5.3.apk` | `A3:72:F9:5E:9E:EC:57:46:A2:66:8C:DA:F3:1C:56:6A:FC:97:39:B0:A4:49:A5:C1:6A:FA:66:86:52:4E:31:EE` |

Para verificar la firma vos mismo:

```
keytool -printcert -jarfile MGGX-Laberinto-X.Y.Z.apk
```

Tiene que dar la misma huella SHA-256 que la tabla de arriba (es siempre la
misma: todas las versiones se firman con la misma clave del repo).

