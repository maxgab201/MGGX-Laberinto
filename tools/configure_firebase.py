#!/usr/bin/env python3
"""Materializa configuracion Firebase sin imprimir sus valores en logs de CI."""
import argparse
import json
import os
from pathlib import Path

parser = argparse.ArgumentParser()
parser.add_argument('--required', action='store_true')
args = parser.parse_args()
target = Path(__file__).resolve().parents[1] / 'app/google-services.json'
raw = os.environ.get('GOOGLE_SERVICES_JSON', '').strip()
if not raw and target.exists():
    raw = target.read_text()
if not raw:
    if args.required:
        raise SystemExit('Falta GOOGLE_SERVICES_JSON. La release con multijugador requiere configuracion Firebase.')
    print('Compilacion de prueba sin Firebase: solo modo solitario.')
else:
    try:
        config = json.loads(raw)
        assert config['project_info']['firebase_url'].startswith('https://')
        assert any(c.get('client_info', {}).get('android_client_info', {}).get('package_name') ==
                   'com.mggx.laberinto' for c in config['client'])
    except (ValueError, KeyError, TypeError, AssertionError, AttributeError):
        raise SystemExit('Configuracion Firebase invalida: revisar JSON, URL de Realtime Database y paquete Android.')
    target.write_text(json.dumps(config, indent=2) + '\n')
    print('Configuracion Firebase validada para la compilacion.')
