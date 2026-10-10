# Autoinicio del vigilante de mazos

Arranca `vigilante_decks.py` al iniciar sesion de Windows, sin ventana de
consola y sin intervencion manual.

## Puesta en marcha

```powershell
py -3 instalar-autoinicio.py
```

## Quitarlo

```powershell
py -3 instalar-autoinicio.py --desinstalar
```

Desinstalar la tarea **no** para el vigilante que este corriendo: el lanzador
no termina procesos a proposito, para no interrumpir una ordenacion en curso.
El script avisa del PID y de como pararlo a mano.

## Por que hay dos piezas

| Archivo | Para que |
|---|---|
| `INICIAR-VIGILANTE-DECKS.cmd` | Ejecuta el vigilante con `pythonw` y anota en un log cuando arranca y cuando termina |
| `comprobar-vigilante.ps1` | Evita un segundo vigilante si ya hay uno lanzado a mano |

El lanzador **no termina** hasta que el vigilante termina. Asi la tarea queda
en estado "En ejecucion" mientras el vigilante viva, lo que hace fiable su
politica de instancia unica y deja el historial de la tarea reflejando si el
vigilante esta sano.

## Instancia unica en dos capas

1. `IgnoreNew` en la tarea: no hay dos arranques simultaneos de la tarea.
2. `comprobar-vigilante.ps1`: si el vigilante ya corre, se registra
   `OMITIDO` y no se arranca otro. Nunca se mata un vigilante en marcha.

## Rutas

```
Vigilante : J:\MTG\Instalacion-XMage\xmage\mage-client\sample-decks\Descargados\vigilante_decks.py
Interprete: C:\Users\vros0\AppData\Local\Programs\Python\Python314\pythonw.exe
Log       : J:\MTG\Instalacion-XMage\xmage\mage-client\config\deck-downloader\vigilante-autoinicio.log
```

`pythonw.exe` evita la ventana de consola. El vigilante es puro `stdlib`, asi
que la version de Python no afecta a su comportamiento.

Si se cambia de ruta, editar las constantes `VIGILANTE`, `PYTHONW` y `LOG` de
`instalar-autoinicio.py`.

## Ajustes de la tarea

| Ajuste | Valor | Por que |
|---|---|---|
| Activador | Al iniciar sesion | Sin privilegios de administrador |
| Instancias multiples | `IgnoreNew` | Nunca dos vigilantes de la tarea |
| Limite de tiempo | Sin limite | El vigilante es de larga duracion |
| Reinicio tras fallo | 3 intentos cada 5 min | Recupera caidas sin bucle infinito |
| Al iniciar | Omitir con bateria baja | No drena el portatil |
| Oculta | Si | Sin ventana emergente |

## Consumo

Medido: ~14 MB de RAM y ~0,04 % de un nucleo en reposo (sondea cada 3 s).

## Pruebas

`tests_autoinicio.py` monta su propio arbol temporal y ejecuta el lanzador de
verdad. No toca la biblioteca de mazos ni los logs de produccion.

```powershell
py -3 tests_autoinicio.py
```

## Pendiente

El activador "Al iniciar sesion" se valida en el proximo inicio de sesion de
Windows. En las pruebas se arranco la tarea a mano, no mediante un reinicio
real del sistema.
