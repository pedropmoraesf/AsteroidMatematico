from pathlib import Path
import base64
import zlib

root = Path(__file__).resolve().parents[1]
payload = ''.join((root / 'tools' / f'.gamev2_payload_{i}').read_text().strip() for i in range(1, 5))
data = zlib.decompress(base64.b64decode(payload))
target = root / 'src' / 'br' / 'grassinimoraes' / 'divasteroides' / 'GameV2Activity.java'
target.write_bytes(data)
print(f'wrote {target} ({len(data)} bytes)')
