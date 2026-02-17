from PIL import Image, ImageDraw, ImageFont
from typing import List, Union


def create_svg_icon(size: int, color: str, prefix: str) -> None:
    """
    Create an SVG test icon with size label and format text.

    Args:
        size: Icon size in pixels
        color: Color for frame and text
        prefix: File name prefix
    """
    # Calculate font sizes (matching PIL version)
    size_font_size = int(size / 3.5)
    format_font_size = int(size / 6)
    line_spacing = int(size / 20)

    # Approximate text widths for centering (using monospace estimation)
    size_text = str(size)
    format_text = "SVG"

    # Create SVG content
    svg_content = f'''<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" width="{size}" height="{size}" viewBox="0 0 {size} {size}">
  <!-- White background -->
  <rect width="{size}" height="{size}" fill="white"/>

  <!-- Frame -->
  <rect x="0.5" y="0.5" width="{size - 1}" height="{size - 1}" fill="none" stroke="{color}" stroke-width="1"/>

  <!-- Size text (centered) -->
  <text x="{size / 2}" y="{size / 2 - format_font_size / 2 - line_spacing}" 
        font-family="Arial, sans-serif" font-size="{size_font_size}" 
        fill="{color}" text-anchor="middle" dominant-baseline="middle">
    {size_text}
  </text>

  <!-- Format text -->
  <text x="{size / 2}" y="{size / 2 + size_font_size / 2 + line_spacing}" 
        font-family="Arial, sans-serif" font-size="{format_font_size}" 
        fill="{color}" text-anchor="middle" dominant-baseline="middle">
    {format_text}
  </text>
</svg>'''

    filename = f"{prefix}{size}x{size}.svg"
    with open(filename, 'w', encoding='utf-8') as f:
        f.write(svg_content)
    print(f"Generated {filename}")


def create_test_icons(
    size: int = 64,
    color: str = 'black',
    format: str = 'webp',
    prefix: str = 'icon_'
) -> None:
    """
    Create test icons with size labels and colored frames.

    Args:
        size: Size of the icon in pixels (default: 64)
        color: Color for the frame and text (default: 'green')
        format: Output format - 'webp', 'png', 'svg', or any PIL-supported 
                format like 'jpeg', 'gif', 'ico', 'avif', 'tiff', 'bmp' (default: 'webp')
        prefix: File name prefix (default: 'icon_')
    """

    # Handle SVG format separately (not a PIL format)
    if format.lower() == 'svg':
        create_svg_icon(size, color, prefix)
        return

    # Create a new image with white background
    img = Image.new('RGB', (size, size), color='white')
    draw = ImageDraw.Draw(img)

    # Draw 1-pixel frame with specified color
    draw.rectangle((0, 0, size - 1, size - 1), outline=color, width=1)

    # Prepare text lines
    size_text = f"{size}"
    format_text = format.upper()

    # Scale font sizes based on icon size
    size_font_size = int(size / 3.5)
    format_font_size = int(size / 6)
    try:
        size_font = ImageFont.truetype("arial.ttf", size_font_size)
        format_font = ImageFont.truetype("arial.ttf", format_font_size)
    except IOError:
        # Fallback to default font if arial.ttf is not found
        size_font = ImageFont.load_default()
        format_font = ImageFont.load_default()

    # Calculate size text bounding box
    size_bbox = draw.textbbox((0, 0), size_text, font=size_font)
    size_text_width = size_bbox[2] - size_bbox[0]
    size_text_height = size_bbox[3] - size_bbox[1]

    # Calculate format text bounding box
    format_bbox = draw.textbbox((0, 0), format_text, font=format_font)
    format_text_width = format_bbox[2] - format_bbox[0]
    format_text_height = format_bbox[3] - format_bbox[1]

    # Calculate total height of both lines with some spacing
    line_spacing = int(size / 20)
    total_height = size_text_height + format_text_height + line_spacing

    # Calculate starting Y position to center both lines vertically
    start_y = (size - total_height) / 2

    # Draw size text centered
    draw.text(((size - size_text_width) / 2, start_y), size_text, fill=color, font=size_font)

    # Draw format text centered below size text
    draw.text(((size - format_text_width) / 2, start_y + size_text_height + line_spacing), format_text, fill=color, font=format_font)

    # Save in the specified format
    format = format.lower()
    try:
        filename = f"{prefix}{size}x{size}.{format}"
        img.save(filename, format.upper())
        print(f"Generated {filename}")
    except Exception as e:
        print(f"Warning: Could not save as {format}: {e}")


if __name__ == '__main__':
    create_test_icons(size=32,  color='green',  format='webp', prefix='item_')
    create_test_icons(size=64,  color='green',  format='webp', prefix='item_')
    create_test_icons(size=128, color='green',  format='webp', prefix='item_')
    create_test_icons(size=256, color='green',  format='webp', prefix='item_')
    create_test_icons(size=512, color='green',  format='webp', prefix='item_')
    create_test_icons(size=64,  color='blue',   format='png',  prefix='item_')
    create_test_icons(size=64,  color='orange', format='ico',  prefix='item_')
    create_test_icons(size=64,  color='red',    format='bmp',  prefix='item_')
    create_test_icons(size=64,  color='purple', format='svg',  prefix='item_')
    create_test_icons(size=128, color='purple', format='svg',  prefix='item_')

    # Example usages:
    # create_test_icons(sizes=[16, 32, 64], color='blue', format='webp')
    # create_test_icons(sizes=[128, 256], color='red', format='png')
    # create_test_icons(sizes=[32, 64], color='purple', format='jpeg', prefix='test_')
    # create_test_icons(sizes=[16, 32, 48, 64, 128, 256], color='navy', format='ico')
    # create_test_icons(sizes=[64, 128, 256], color='teal', format='svg', prefix='vector_')
