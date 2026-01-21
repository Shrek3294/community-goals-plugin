import os
from PIL import Image

def hex_to_rgb(hex_color):
    hex_color = hex_color.lstrip('#')
    return tuple(int(hex_color[i:i+2], 16) for i in (0, 2, 4))

def interpolate_color(val, min_val, max_val, color_start, color_end):
    t = (val - min_val) / (max_val - min_val)
    t = max(0, min(1, t))
    return tuple(int(color_start[i] + (color_end[i] - color_start[i]) * t) for i in range(3))

def main():
    source_path = "iron_ingot.png"
    target_path = "catalyst_resourcepack/assets/minecraft/textures/item/catalyst_ingot.png"
    
    # Palette
    # Base: #8B5CF6 (139, 92, 246)
    # Shadow: #5B21B6 (91, 33, 182)
    # Highlight: #C4B5FD (196, 181, 253)
    
    shadow_rgb = hex_to_rgb("#5B21B6")
    base_rgb = hex_to_rgb("#8B5CF6")
    highlight_rgb = hex_to_rgb("#C4B5FD")
    
    if not os.path.exists(source_path):
        print(f"Error: {source_path} not found.")
        return

    try:
        img = Image.open(source_path).convert("RGBA")
    except Exception as e:
        print(f"Error opening image: {e}")
        return

    width, height = img.size
    new_img = Image.new("RGBA", (width, height))
    pixels = new_img.load()
    
    # Analyze source brightness to map correctly
    # Iron ingot usually has a range of grays.
    # We will assume a standard range for mapping.
    # Or simply: < 128 -> Shadow to Base, > 128 -> Base to Highlight
    
    for y in range(height):
        for x in range(width):
            r, g, b, a = img.getpixel((x, y))
            
            if a == 0:
                pixels[x, y] = (0, 0, 0, 0)
                continue
            
            # Simple brightness
            brightness = (r + g + b) / 3.0
            
            # Map brightness
            # Assuming iron ingot brightness roughly 50 to 255
            # We'll use a threshold to decide between shadow-base and base-highlight gradients
            
            # Adjust these thresholds if needed based on standard iron ingot
            if brightness < 128:
                # Shadow to Base
                new_col = interpolate_color(brightness, 50, 128, shadow_rgb, base_rgb)
            else:
                # Base to Highlight
                new_col = interpolate_color(brightness, 128, 230, base_rgb, highlight_rgb)
                
            pixels[x, y] = (*new_col, a)
            
    # Ensure directory exists
    os.makedirs(os.path.dirname(target_path), exist_ok=True)
    
    new_img.save(target_path)
    print(f"Created {target_path}")

if __name__ == "__main__":
    main()
