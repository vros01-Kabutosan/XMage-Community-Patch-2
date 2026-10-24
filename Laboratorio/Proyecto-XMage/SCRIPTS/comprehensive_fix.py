#!/usr/bin/env python3
"""
Comprehensive fix for folder update issue with deck downloads.
This addresses the common problem where downloaded decks don't update folders.
"""

import os
import sys
import time
import glob
from pathlib import Path


def download_deck_with_folder_update(base_dir, folder_name, deck_data, filename="deck.json"):
    """
    Download/save a deck and ensure the folder is properly updated.
    
    This function addresses the common issue where folders don't reflect
    new deck downloads due to:
    - Cached directory listings
    - Missing absolute path conversion
    - Insufficient filesystem sync time
    """
    
    # Create folder path using Path for cross-platform compatibility
    folder_path = Path(base_dir) / folder_name
    
    # Ensure folder exists - create if needed
    folder_path.mkdir(parents=True, exist_ok=True)
    
    # Save the deck file
    deck_file = folder_path / filename
    with open(deck_file, 'w', encoding='utf-8') as f:
        if isinstance(deck_data, str):
            f.write(deck_data)
        else:
            # Assume it's dict/list for JSON
            import json
            json.dump(deck_data, f, ensure_ascii=False, indent=2)
    
    # CRITICAL: Force folder refresh after write
    # 1. Convert to absolute path to ensure fresh reference
    abs_folder = os.path.abspath(folder_path)
    
    # 2. Small delay to let filesystem settle
    time.sleep(0.1)
    
    # 3. Force re-read of directory contents
    # This clears any cached directory listing
    try:
        current_contents = sorted(os.listdir(abs_folder))
        print(f"✓ Carpeta actualizada: {abs_folder}")
        print(f"  Total de archivos: {len(current_contents)}")
        
        # 4. Verify the new file is visible
        if filename in current_contents:
            print(f"✓ Archivo {filename} confirmado en la carpeta")
        else:
            print(f"⚠ Advertencia: {filename} no visible en lista inmediata")
            # Try one more time with longer delay
            time.sleep(0.2)
            current_contents = sorted(os.listdir(abs_folder))
            if filename in current_contents:
                print(f"✓ Archivo {filename} confirmado después de segundo intento")
    except Exception as e:
        print(f"✗ Error al leer carpeta: {e}")
        current_contents = []
    
    return abs_folder, current_contents


def batch_download_decks(decks_info, base_dir="."):
    """
    Download multiple decks and ensure all folders are updated.
    
    decks_info: list of dicts with keys:
        - folder_name: name of folder for this deck
        - data: deck data to save
        - filename: optional filename (defaults to deck_name.json)
    """
    
    print(f"=== Descarga por lotes de {len(decks_info)} decks ===")
    print(f"Directorio base: {os.path.abspath(base_dir)}\n")
    
    results = []
    
    for i, deck_info in enumerate(decks_info, 1):
        folder_name = deck_info.get("folder_name", f"deck_{i}")
        deck_data = deck_info.get("data")
        filename = deck_info.get("filename", f"{folder_name}.json")
        
        print(f"[{i}/{len(decks_info)}] Descargando a: {folder_name}")
        
        abs_path, contents = download_deck_with_folder_update(
            base_dir, folder_name, deck_data, filename
        )
        
        results.append({
            "folder": folder_name,
            "absolute_path": abs_path,
            "files": contents,
            "success": filename in contents
        })
        
        # Brief pause between downloads to prevent filesystem overload
        if i < len(decks_info):
            time.sleep(0.05)
    
    # Summary
    print("\n" + "=" * 60)
    print("RESUMEN DE DESCARGAS")
    print("=" * 60)
    
    success_count = sum(1 for r in results if r["success"])
    print(f"Total: {len(results)} decks")
    print(f"Exitosos: {success_count}")
    print(f"Fallidos: {len(results) - success_count}")
    
    for r in results:
        status = "✓" if r["success"] else "✗"
        print(f"{status} {r['folder']}: {len(r['files'])} archivos")
    
    return results


# Example usage
if __name__ == "__main__":
    # Example deck data
    example_decks = [
        {
            "folder_name": "modern",
            "data": {"name": "Modern Deck", "cards": ["Lightning Bolt", "Snapcaster Mage"]},
            "filename": "modern_deck.json"
        },
        {
            "folder_name": "legacy",
            "data": {"name": "Legacy Deck", "cards": ["Mox Opal", "Seat of the Synod"]},
            "filename": "legacy_deck.json"
        }
    ]
    
    # Run batch download
    batch_download_decks(example_decks, base_dir="./decks")