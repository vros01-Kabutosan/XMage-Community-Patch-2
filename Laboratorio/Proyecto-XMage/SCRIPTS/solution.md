# Solución para actualización de carpetas con descargas de decks

## Problema identificado
Los archivos de descarga de decks no están actualizando correctamente las carpetas después de descargar contenido.

## Posibles causas y soluciones

### 1. **Atributo _dirty flag desactivado**
Si estás usando `watchdir` o monitoreando cambios, asegúrate de que el atributo `_dirty` esté configurado correctamente.

```python
import os
import time

# Forzar refresco del directorio
def refresh_directory(path):
    """Forzar refresco del directorio"""
    if os.path.exists(path):
        # Leer y actualizar el listado
        contents = os.listdir(path)
        return contents
    return []
```

### 2. **Sincronización de escritura**
Asegúrate de esperar a que termine la escritura del archivo antes de refrescar:

```python
import time

def wait_for_write_completion(file_path, timeout=10):
    """Esperar a que termine la escritura del archivo"""
    start_time = time.time()
    last_size = -1
    
    while time.time() - start_time < timeout:
        try:
            current_size = os.path.getsize(file_path)
            if current_size == last_size:
                time.sleep(0.1)
                current_size = os.path.getsize(file_path)
                if current_size == last_size:
                    return True  # Escritura completada
            last_size = current_size
            time.sleep(0.1)
        except OSError:
            time.sleep(0.1)
    
    return False
```

### 3. **Actualizar variable de carpeta**
Si usas una variable para almacenar la ruta de la carpeta, asegúrate de actualizarla:

```python
import os

class DeckDownloader:
    def __init__(self, base_path):
        self.base_path = base_path
        self.folder_cache = {}
    
    def download_and_save(self, deck_data, folder_name):
        """Descargar y guardar deck"""
        folder_path = os.path.join(self.base_path, folder_name)
        
        # Crear carpeta si no existe
        os.makedirs(folder_path, exist_ok=True)
        
        # Guardar deck
        deck_path = os.path.join(folder_path, "deck.txt")
        with open(deck_path, 'w') as f:
            f.write(deck_data)
        
        # Forzar actualización
        self.folder_cache.pop(folder_name, None)
        
        # Refrescar lista
        return os.listdir(folder_path)
```

### 4. **Solución completada - Actualiza la variable de carpeta**

En el archivo principal, busca dónde se define la variable que almacena la ruta de las carpetas de descargas. Asegúrate de que después de cada descarga, esta variable se actualice correctamente.

Ejemplo de código problemático:
```python
carpeta_descargas = "ruta/inicial"
# ... hacer descargas ...
# Falta actualizar carpeta_descargas
```

Solución:
```python
carpeta_descargas = "ruta/inicial"
# ... hacer descargas ...
carpeta_descargas = os.path.abspath(carpeta_descargas)  # Actualizar ruta
```

## Pasos a seguir

1. Busca en los 3 archivos Python por:
   - `os.listdir`
   - `glob.*`
   - Variables que almacenen rutas de carpetas
   - Funciones de descarga

2. Verifica que después de cada descarga, se esté:
   - Creando el directorio si no existe
   - Guardando el archivo correctamente
   - Actualizando la referencia de la carpeta

3. Agrega código para forzar refresco del directorio después de cada descarga

## Código de ejemplo para implementar

```python
import os
import shutil

def ensure_folder_updated(folder_path, folder_name=None):
    """
    Garantiza que la carpeta esté actualizada y los cambios se reflejen
    """
    if folder_name:
        folder_path = os.path.join(folder_path, folder_name)
    
    # Normalizar ruta
    folder_path = os.path.abspath(folder_path)
    
    # Crear si no existe
    os.makedirs(folder_path, exist_ok=True)
    
    # Forzar sincronización del sistema de archivos
    os.sync()  # En sistemas Unix
    
    # En Windows, usar un pequeño delay
    time.sleep(0.1)
    
    # Verificar que se puede leer el directorio
    try:
        contents = os.listdir(folder_path)
        print(f"Carpeta actualizada: {folder_path} - {len(contents)} archivos")
        return folder_path
    except Exception as e:
        print(f"Error al actualizar carpeta: {e}")
        return folder_path