#!/usr/bin/env python3
"""
Prueba que el relay de multijugador ande de verdad, contra tu Firebase.

Simula dos jugadores en la misma sala hablando el protocolo real del juego
(el de net/NetProtocol.kt): uno manda, el otro tiene que recibir. Es la misma
plomeria que usa TransporteFirebase, nomas que por HTTP en vez del SDK, asi
se puede correr desde cualquier lado sin un telefono.

Uso:
    python3 tools/probar_relay.py

No necesita nada instalado: usa solo la libreria estandar. Saca la direccion
de la base de app/google-services.json, asi que anda con el proyecto que
tengas configurado, sin tocar nada aca.
"""

import json
import os
import sys
import time
import urllib.error
import urllib.request

RAIZ = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CONFIG = os.path.join(RAIZ, "app", "google-services.json")

# Una sala aparte, con nombre feo a proposito para no pisarle la sala a nadie
# que este jugando de verdad mientras se corre esta prueba.
SALA = "ZZPRUEBA"


def pedir(metodo, url, cuerpo=None):
    """Una llamada HTTP a la base. Devuelve (codigo, texto)."""
    datos = json.dumps(cuerpo).encode() if cuerpo is not None else None
    req = urllib.request.Request(url, data=datos, method=metodo)
    req.add_header("Content-Type", "application/json")
    try:
        with urllib.request.urlopen(req, timeout=20) as r:
            return r.status, r.read().decode()
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode()
    except Exception as e:  # sin internet, DNS caido, lo que sea
        return 0, str(e)


def main():
    if not os.path.exists(CONFIG):
        print("FALTA app/google-services.json.")
        print("Ver el Paso 1 de docs/MULTIJUGADOR.md: hay que crear el")
        print("proyecto en la consola de Firebase y bajar ese archivo.")
        return 1

    with open(CONFIG) as f:
        cfg = json.load(f)
    base = cfg.get("project_info", {}).get("firebase_url")
    if not base:
        print("El google-services.json no tiene 'firebase_url'.")
        print("Eso pasa cuando el proyecto no tiene Realtime Database creada.")
        print("Ver el paso 2 del Paso 1 en docs/MULTIJUGADOR.md.")
        return 1

    msgs = f"{base}/salas/{SALA}/msgs.json"
    print(f"Base:  {base}")
    print(f"Sala:  {SALA}\n")

    fallos = []

    # --- 1) El jugador 1 entra y manda unas cosas -----------------------
    # Mismo formato que arma NetProtocol.kt: VERSION|TIPO|quien|argumentos.
    salida = [
        "1|JOIN|jug1|Tester Uno|skin_minero",
        "1|START|jug1|CARRERA|3|987654321",
        "1|POSE|jug1|4.50|0.00|7.50|90.00|0",
    ]
    print("1) El jugador 1 manda sus mensajes")
    for m in salida:
        codigo, cuerpo = pedir("POST", msgs, {"m": m, "t": {".sv": "timestamp"}})
        if codigo != 200:
            fallos.append(f"no se pudo escribir ({codigo}): {cuerpo.strip()}")
            print(f"   FALLO  {m}")
        else:
            print(f"   ok     {m}")

    if fallos:
        print("\nNo se puede escribir en la base. Casi seguro son las reglas:")
        print("revisa que esten las de docs/MULTIJUGADOR.md (Paso 1, punto 3)")
        print("y que las hayas guardado con Publicar.")
        return 1

    # --- 2) El jugador 2 lee lo que llego --------------------------------
    print("\n2) El jugador 2 lee la sala")
    codigo, cuerpo = pedir("GET", msgs)
    if codigo != 200:
        print(f"   FALLO al leer ({codigo}): {cuerpo.strip()}")
        return 1

    recibidos = [v["m"] for v in (json.loads(cuerpo) or {}).values()]
    for m in salida:
        if m in recibidos:
            print(f"   ok     recibio: {m}")
        else:
            fallos.append(f"no llego el mensaje: {m}")
            print(f"   FALLO  no llego: {m}")

    # --- 3) El indice que usa la poda de mensajes viejos -----------------
    print("\n3) La consulta ordenada por tiempo (la que usa la poda)")
    codigo, cuerpo = pedir("GET", f'{msgs}?orderBy="t"&limitToFirst=5')
    if codigo == 200:
        print("   ok     el indice .indexOn de 't' esta puesto")
    else:
        fallos.append("falta el indice de 't' en las reglas")
        print(f"   FALLO  ({codigo}): {cuerpo.strip()}")
        print("   Agregale '.indexOn': ['t'] a las reglas (ver la guia).")

    # --- 4) Que no se pueda escribir fuera de las salas ------------------
    # No es un capricho: si esto se puede, las reglas quedaron abiertas de
    # mas y cualquiera puede escribir en cualquier lado del proyecto.
    print("\n4) Que las reglas no dejen escribir fuera de las salas")
    codigo, _ = pedir("PUT", f"{base}/zz_prueba_fuera.json", "x")
    if codigo == 401:
        print("   ok     el resto del proyecto esta protegido")
    else:
        print(f"   OJO    se pudo escribir fuera de salas/ (HTTP {codigo})")
        print("   Las reglas quedaron mas abiertas de lo que hace falta.")
        print("   Si el proyecto tiene otra cosa adentro, revisalas.")
        pedir("DELETE", f"{base}/zz_prueba_fuera.json")

    # --- 5) Limpieza ------------------------------------------------------
    pedir("DELETE", msgs)

    print()
    if fallos:
        print("HAY PROBLEMAS:")
        for f in fallos:
            print(f"  - {f}")
        return 1

    print("Anda todo: los mensajes viajan de un jugador al otro.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
