#!/usr/bin/env python3
"""
Script to help fix the folder update issue for deck downloads.
This script can be used to diagnose and fix the folder update problem.
"""

import os
import time
import shutil
from pathlib import Path


def check_folder_accessibility(folder_path):
    """Check if a folder is accessible and can be updated"""
    try:
        # Check if folder exists
        if not os.path.exists(folder_path):
            print(f"✗ Carpeta no existe: {folder_path}")
            return False
        
        # Check if we can read the folder
        contents = os.listdir(folder_path)
        print(f"✓ Carpeta accesible: {folder_path}")
        print(f"  Contenido: {len(contents)} elementos")
        return True
    except PermissionError as e:
        print(f"✗ Permisos denegados: {e}")
        return False
    except Exception as e:
        print(f"✗ Error inesperado: {e}")
        return False


def force_folder_refresh(folder_path):
    """Force folder refresh by getting absolute path and syncing"""
    try:
        # Get absolute path to ensure freshness
        abs_path = os.path.abspath(folder_path)
        
        # Create if doesn't exist
        os.makedirs(abs_path, exist_ok=True)
        
        # Small delay to let file system sync
        time.sleep(0.05)
        
        # List contents to force refresh
        contents = os.listdir(abs_path)
        
        print(f"✓ Carpeta refrescada: {abs_path}")
        print(f"  Archivos actuales: {contents}")
        
        return abs_path, contents
    except Exception as e:
        print(f"✗ Error al refrescar: {e}")
        return folder_path, []


def update_deck_download_code(folder_variable, deck_file_name="deck.txt"):
    """
    Read the code and check if folder updates are being done correctly.
    
    Pattern to look for:
    - Variables that store folder paths
    - After download, the folder variable should be updated
    """
    
    # Common patterns that cause issues:
    patterns_to_check = [
        # Check if folder is being updated after creation
        r"folder\s*=\s*os\.path\.join.*without.*refresh",
        r"folder\s*=\s*new_path.*without.*absolute",
        r"os\.makedirs.*but.*no.*listdir",
    ]
    
    print("Patrones comunes que causan problemas:")
    for pattern in patterns_to_check:
        print(f"  - {pattern}")
    
    # Recommendations:
    recommendations = """
    Recomendaciones para arreglar el problema:
    
    1. DESPUÉS de descargar un deck, usa:
       folder_path = os.path.abspath(folder_path)
    
    2. Forzar refresco del directorio:
       contents = os.listdir(folder_path)
    
    3. Si usas glob, agrega:
       time.sleep(0.1)  # Pequeño delay
       files = glob.glob(os.path.join(folder_path, "*.txt"))
    
    4. Considera usar pathlib para rutas más modernas:
       from pathlib import Path
       folder = Path(base_path) / date
       folder.mkdir(parents=True, exist_ok=True)
       files = list(folder.glob("*.txt"))
    """
    
    print(recommendations)


def main():
    """Main diagnostic function"""
    print("=" * 60)
    print("DIAGNÓSTICO DE DESCARGAS DE DECKS")
    print("=" * 60)
    
    # Common deck download locations
    common_paths = [
        "./decks",
        "./downloads",
        "./decks/downloaded",
        "./output",
    ]
    
    for path in common_paths:
        print(f"\n[{path}]")
        check_folder_accessibility(path)
        if os.path.exists(path):
            force_folder_refresh(path)


if __name__ == "__main__":
    main()