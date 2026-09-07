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
    reversed_edges = []
    for match in re.finditer(r'smoothstep\(\s*([0-9.]+)\s*,\s*([0-9.]+)\s*,', body):
        if float(match[1]) >= float(match[2]): reversed_edges.append(match[0])
    ok = r.returncode == 0 and not reversed_edges
    if reversed_edges: print("smoothstep indefinido:", reversed_edges)
    print(f"{'OK  ' if ok else 'FALLA'} {name} ({stage})")
    if not ok:
        fails += 1
        print(r.stdout)
        print(r.stderr)
    os.unlink(path)

# A stage can compile while its varyings fail to link with the other stage.
shaders = dict(found)
for name, body in found:
    if not name.endswith('_VS'): continue
    fragment = name[:-3] + '_FS'
    if fragment not in shaders:
        fails += 1
        print(f"Falta {fragment}")
        continue
    with tempfile.TemporaryDirectory() as directory:
        vertex = os.path.join(directory, 'shader.vert')
        pixel = os.path.join(directory, 'shader.frag')
        open(vertex, 'w').write(body)
        open(pixel, 'w').write(shaders[fragment])
        result = subprocess.run(['glslangValidator', '-l', vertex, pixel], capture_output=True, text=True)
        print(f"{'OK' if result.returncode == 0 else 'FALLA'} enlace {name[:-3]}")
        if result.returncode:
            fails += 1
            print(result.stdout, result.stderr)

print(f"\n{len(found)} shaders, {fails} con error")
sys.exit(1 if fails else 0)

