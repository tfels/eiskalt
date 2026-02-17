import argparse
import sys
from PIL import Image
import os

# Set up argument parser
parser = argparse.ArgumentParser(
    description='Generate Android app launcher icons in all densities from normal and round input images.',
    formatter_class=argparse.RawDescriptionHelpFormatter,
    epilog='''
Examples:
  python convert.py normal_icon.png round_icon.png
  python convert.py normal_icon.png round_icon.png --generate-adaptive
'''
)

parser.add_argument(
    'normal_input',
    help='Path to the normal launcher icon image file'
)

parser.add_argument(
    'round_input',
    help='Path to the round launcher icon image file'
)

parser.add_argument(
    '--generate-adaptive',
    action='store_true',
    help='Generate adaptive icon XML files in mipmap-anydpi-v26 folder'
)

# Parse arguments
args = parser.parse_args()

normal_input = args.normal_input
round_input = args.round_input
generate_adaptive = args.generate_adaptive

# Hardcoded output settings
dir_template = 'mipmap-{density}'
output_names = ['ic_launcher', 'ic_launcher_round']

# Define the Android icon sizes (width, height) corresponding to densities
sizes = {
    'mdpi': (48, 48),
    'hdpi': (72, 72),
    'xhdpi': (96, 96),
    'xxhdpi': (144, 144),
    'xxxhdpi': (192, 192)
}

# Input files and corresponding output names
inputs_and_outputs = [
    (normal_input, 'ic_launcher'),
    (round_input, 'ic_launcher_round')
]

# Process each input file
for input_file, output_name in inputs_and_outputs:
    # Open the input image
    try:
        img = Image.open(input_file)
    except FileNotFoundError:
        print(f"Error: File '{input_file}' not found.")
        sys.exit(1)

    # Create resized versions for each density
    for density, size in sizes.items():
        # Create density folder if it doesn't exist
        density_folder = dir_template.format(density=density)
        os.makedirs(density_folder, exist_ok=True)

        # Resize image
        resized_img = img.resize(size, Image.Resampling.LANCZOS)

        # Save with consistent output name
        output_filename = f"{output_name}.webp"
        output_path = os.path.join(density_folder, output_filename)
        resized_img.save(output_path, "WEBP")
        print(f"Created {output_path}")

# Generate adaptive icon XML files if requested
if generate_adaptive:
    adaptive_folder = "mipmap-anydpi-v26"
    os.makedirs(adaptive_folder, exist_ok=True)

    # Generate regular adaptive icon XML
    adaptive_xml = '''<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
    <monochrome android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>'''

    adaptive_path = os.path.join(adaptive_folder, "ic_launcher.xml")
    with open(adaptive_path, 'w', encoding='utf-8') as f:
        f.write(adaptive_xml)
    print(f"Created {adaptive_path}")

    # Generate round adaptive icon XML
    adaptive_round_xml = '''<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
    <monochrome android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>'''

    adaptive_round_path = os.path.join(adaptive_folder, "ic_launcher_round.xml")
    with open(adaptive_round_path, 'w', encoding='utf-8') as f:
        f.write(adaptive_round_xml)
    print(f"Created {adaptive_round_path}")

print("Icon generation complete!")
