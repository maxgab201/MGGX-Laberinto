#!/usr/bin/env python3
"""Extrae los shaders de Shaders.kt y los valida con glslangValidator."""
import re, subprocess, sys, tempfile, os

src = open('app/src/main/java/com/mggx/laberinto/gl/Shaders.kt').read()
pat = re.compile(r'const val (\w+) = """(.*?)"""', re.S)
found = pat.findall(src)
if not found:
    print("NO se encontro ningun shader"); sys.exit(1)

fails = 0
for name, body in found:
    stage = 'vert' if name.endswith('_VS') else 'frag'
    ext = '.vert' if stage == 'vert' else '.frag'
    with tempfile.NamedTemporaryFile('w', suffix=ext, delete=False) as f:
        f.write(body)
        path = f.name
    r = subprocess.run(['glslangValidator', '-S', stage, path],
                       capture_output=True, text=True)
    ok = r.returncode == 0
    print(f"{'OK  ' if ok else 'FALLA'} {name} ({stage})")
    if not ok:
        fails += 1
        print(r.stdout)
        print(r.stderr)
    os.unlink(path)

print(f"\n{len(found)} shaders, {fails} con error")
sys.exit(1 if fails else 0)
