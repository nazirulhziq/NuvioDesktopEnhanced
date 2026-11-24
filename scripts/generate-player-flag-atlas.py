"""Build offline flags with /usr/bin/python3 and the npm flag-icons@7.3.2 tarball."""
import io
import re
import sys
import tarfile
from pathlib import Path

import cairo
import gi
gi.require_version('Rsvg', '2.0')
from gi.repository import Rsvg
from PIL import Image

out = Path(__file__).resolve().parents[1] / 'composeApp/src/desktopMain/resources/player-ui'
w, h = 48, 36
atlas = Image.new('RGBA', (26 * w, 26 * h))
with tarfile.open(sys.argv[1]) as package:
    for member in package.getmembers():
        m = re.fullmatch(r'package/flags/4x3/([a-z])([a-z])\.svg', member.name)
        if not m:
            continue
        svg = Rsvg.Handle.new_from_data(package.extractfile(member).read())
        tile = cairo.ImageSurface(cairo.FORMAT_ARGB32, w, h)
        viewport = Rsvg.Rectangle(); viewport.width, viewport.height = w, h
        svg.render_document(cairo.Context(tile), viewport)
        data = io.BytesIO(); tile.write_to_png(data); data.seek(0)
        atlas.paste(Image.open(data), ((ord(m[2])-97)*w, (ord(m[1])-97)*h))
    (out / 'flag-icons-LICENSE.txt').write_bytes(package.extractfile('package/LICENSE').read())
atlas.save(out / 'language-flags.png', optimize=True)
common = out.parents[2] / 'commonMain/composeResources'
(common / 'files').mkdir(exist_ok=True)
(common / 'drawable/language_flags.png').write_bytes((out / 'language-flags.png').read_bytes())
(common / 'files/flag-icons-LICENSE.txt').write_bytes((out / 'flag-icons-LICENSE.txt').read_bytes())
