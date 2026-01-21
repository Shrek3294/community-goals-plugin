import os
import random
from PIL import Image

# Configuration
BASE_DIR = r"c:\Users\matte\Desktop\Community Plugin\catalyst_resourcepack\assets\minecraft\textures\block"
FILES = {
    "side": "reinforced_deepslate_side.png",
    "top": "reinforced_deepslate_top.png",
    "bottom": "reinforced_deepslate_bottom.png"
}

# Palettes
DEEPSLATE_PALETTE = [
    (30, 30, 34),    # #1E1E22
    (37, 37, 42),    # #25252A
    (44, 44, 50),    # #2C2C32
    (51, 51, 58),    # #33333A
    (59, 59, 68),    # #3B3B44
    (69, 69, 80)     # #454550
]

ORE_PALETTE = {
    "base": (139, 92, 246),   # #8B5CF6
    "shadow": (91, 33, 182),  # #5B21B6
    "light": (196, 181, 253), # #C4B5FD
    "sparkle": (245, 243, 255)# #F5F3FF (Rare)
}

def generate_deepslate_base():
    """Generates a 16x16 deepslate texture using low-frequency patches."""
    im = Image.new("RGB", (16, 16))
    pixels = im.load()
    
    # Create simple Voronoi-like patches
    num_seeds = 8
    seeds = []
    for _ in range(num_seeds):
        rx = random.randint(0, 15)
        ry = random.randint(0, 15)
        color = random.choice(DEEPSLATE_PALETTE)
        seeds.append(((rx, ry), color))
    
    for x in range(16):
        for y in range(16):
            best_dist = 999
            best_color = DEEPSLATE_PALETTE[0]
            for (sx, sy), color in seeds:
                dist = ((x - sx)**2 + (y - sy)**2)
                if dist < best_dist:
                    best_dist = dist
                    best_color = color
            pixels[x, y] = best_color
            
    # Add slight noise to break edges
    for x in range(16):
        for y in range(16):
            if random.random() < 0.15:
                 pixels[x, y] = random.choice(DEEPSLATE_PALETTE)

    # Add Dark Pockets
    num_pockets = random.randint(3, 5)
    darkest_color = DEEPSLATE_PALETTE[0] # #1E1E22
    for _ in range(num_pockets):
        px, py = random.randint(1, 14), random.randint(1, 14)
        pixels[px, py] = darkest_color
        nx, ny = px + random.randint(-1, 1), py + random.randint(-1, 1)
        if 0 <= nx < 16 and 0 <= ny < 16:
             pixels[nx, ny] = darkest_color

    return im

def generate_cluster(pixels, occupied, max_size=6):
    """Generates a single ore cluster."""
    attempts = 0
    while attempts < 20:
        sx = random.randint(0, 15)
        sy = random.randint(0, 15)
        if (sx, sy) not in occupied:
            break
        attempts += 1
    else:
        return 

    cluster_pixels = [(sx, sy)]
    occupied.add((sx, sy))
    
    current_size = 1
    target_size = random.randint(2, max_size)
    
    while current_size < target_size:
        px, py = random.choice(cluster_pixels)
        nx, ny = px + random.randint(-1, 1), py + random.randint(-1, 1)
        
        if 0 <= nx < 16 and 0 <= ny < 16:
            if (nx, ny) not in occupied:
                cluster_pixels.append((nx, ny))
                occupied.add((nx, ny))
                current_size += 1
    
    highlights_placed = 0
    for cx, cy in cluster_pixels:
        r = random.random()
        if r < 0.3: 
            color = ORE_PALETTE["shadow"]
        elif r > 0.85 and highlights_placed < 2: 
            if random.random() < 0.05:
                 color = ORE_PALETTE["sparkle"]
            else:
                 color = ORE_PALETTE["light"]
            highlights_placed += 1
        else:
            color = ORE_PALETTE["base"]
        pixels[cx, cy] = color

def generate_natural_ore(img_name, face_type):
    path = os.path.join(BASE_DIR, img_name)
    
    img = generate_deepslate_base()
    pixels = img.load()
    occupied = set()

    # Increase Density for "More Veins"
    if face_type == "side":
        num_clusters = random.randint(6, 10) # Was 4-8
    elif face_type == "top":
        num_clusters = random.randint(3, 6)  # Was 1-4
    elif face_type == "bottom":
        num_clusters = random.randint(2, 4)  # Was 1-3
    else:
        num_clusters = 3

    print(f"Generating {face_type}: {num_clusters} clusters")
    for _ in range(num_clusters):
        generate_cluster(pixels, occupied)

    try:
        if not os.path.exists(BASE_DIR):
            os.makedirs(BASE_DIR) 
        img.save(path)
        print(f"Saved {img_name} ({face_type})")
    except Exception as e:
        print(f"Error saving {img_name}: {e}")

def main():
    print("Starting Natural Ore Generation (High Density)...")
    random.seed()
    generate_natural_ore(FILES["side"], "side")
    generate_natural_ore(FILES["top"], "top")
    generate_natural_ore(FILES["bottom"], "bottom")
    print("Done.")

if __name__ == "__main__":
    main()
