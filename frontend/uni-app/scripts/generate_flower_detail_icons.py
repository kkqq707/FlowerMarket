"""Render the small line icons used by the flower-detail reference layout."""

from pathlib import Path

from PIL import Image, ImageDraw


SCALE = 12
OUTPUT = Path(__file__).resolve().parents[1] / "static" / "images" / "flower-detail"
INK = "#485748"
SAGE = "#A7B39F"


def point(value):
    return round(value * SCALE)


def bezier(start, control_a, control_b, end, count=24):
    points = []
    for index in range(count + 1):
        t = index / count
        u = 1 - t
        x = u**3 * start[0] + 3 * u**2 * t * control_a[0] + 3 * u * t**2 * control_b[0] + t**3 * end[0]
        y = u**3 * start[1] + 3 * u**2 * t * control_a[1] + 3 * u * t**2 * control_b[1] + t**3 * end[1]
        points.append((point(x), point(y)))
    return points


def stroke(draw, points, color=INK, width=1.65):
    draw.line(points, fill=color, width=point(width), joint="curve")


def curve(draw, start, control_a, control_b, end, color=INK, width=1.65):
    stroke(draw, bezier(start, control_a, control_b, end), color, width)


def save(image, name, size):
    OUTPUT.mkdir(parents=True, exist_ok=True)
    image.resize((size * 3, size * 3), Image.Resampling.LANCZOS).save(OUTPUT / name, optimize=True)


def chat_icon():
    image = Image.new("RGBA", (point(32), point(32)), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    path = [(point(7), point(6)), (point(25), point(6))]
    path += bezier((25, 6), (27.5, 6), (29, 7.5), (29, 10))[1:]
    path.append((point(29), point(19)))
    path += bezier((29, 19), (29, 21.5), (27.5, 23), (25, 23))[1:]
    path.extend([(point(14), point(23)), (point(9), point(27)), (point(9), point(23)), (point(7), point(23))])
    path += bezier((7, 23), (4.5, 23), (3, 21.5), (3, 19))[1:]
    path.append((point(3), point(10)))
    path += bezier((3, 10), (3, 7.5), (4.5, 6), (7, 6))[1:]
    stroke(draw, path)
    for x in (10.5, 16, 21.5):
        draw.ellipse((point(x - 1), point(14), point(x + 1), point(16)), fill=INK)
    save(image, "review-chat.png", 32)


def lotus_icon():
    image = Image.new("RGBA", (point(32), point(32)), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    curve(draw, (16, 5), (10.5, 12), (11, 19), (16, 26))
    curve(draw, (16, 5), (21.5, 12), (21, 19), (16, 26))
    curve(draw, (8, 11), (6, 18), (9.5, 23), (16, 26))
    curve(draw, (8, 11), (11, 12.5), (13.3, 17), (14, 21))
    curve(draw, (24, 11), (26, 18), (22.5, 23), (16, 26))
    curve(draw, (24, 11), (21, 12.5), (18.7, 17), (18, 21))
    curve(draw, (3.5, 16), (3.5, 21), (7.5, 25.5), (16, 27))
    curve(draw, (3.5, 16), (5.3, 16.2), (7.2, 17.5), (8.4, 19))
    curve(draw, (28.5, 16), (28.5, 21), (24.5, 25.5), (16, 27))
    curve(draw, (28.5, 16), (26.7, 16.2), (24.8, 17.5), (23.6, 19))
    save(image, "detail-lotus.png", 32)


def sprig_icon():
    image = Image.new("RGBA", (point(72), point(72)), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    curve(draw, (15, 65), (38, 49), (54, 24), (61, 5), SAGE, 1.45)
    leaves = [
        ((27, 55), (17, 50), (15, 43), (17, 38), (25, 42), (28, 48)),
        ((36, 45), (40, 37), (48, 35), (53, 35), (50, 43), (43, 46)),
        ((45, 34), (35, 31), (34, 24), (36, 19), (43, 23), (47, 28)),
        ((52, 22), (54, 14), (61, 10), (66, 9), (64, 17), (58, 21)),
        ((58, 13), (53, 8), (54, 3), (58, 1), (61, 5), (60, 9)),
    ]
    for start, c1, c2, tip, c3, c4 in leaves:
        curve(draw, start, c1, c2, tip, SAGE, 1.25)
        curve(draw, tip, c3, c4, start, SAGE, 1.25)
    save(image, "meaning-sprig.png", 72)


if __name__ == "__main__":
    chat_icon()
    lotus_icon()
    sprig_icon()
