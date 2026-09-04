#!/usr/bin/env python3
"""Arma el JSON del cuerpo de la release. Lo usa publicar_release.sh."""
import json
import sys

version, nombre, cuerpo, salida = sys.argv[1], sys.argv[2], sys.argv[3], sys.argv[4]
with open(salida, "w", encoding="utf-8") as f:
    json.dump({
        "tag_name": "v" + version,
        "name": f"MGGX Laberinto {version} - {nombre}",
        "body": cuerpo,
        "draft": False,
        "prerelease": False,
    }, f)
