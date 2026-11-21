#!/usr/bin/env bash
# Verificador de mods del fork XMage de Kabutosan sobre una build upstream.
#
# Nace de la migracion 1.4.63V1 -> 1.4.63V2 (2026-11-21): la migracion se valido con
# -DskipTests y con un checklist de marcadores que no se guardo en ningun fichero, asi que
# no habia forma de re-verificarla ante la siguiente build. Este script cierra ese hueco.
#
# Hace dos cosas:
#   1) ESTRUCTURAL - el conjunto de ficheros que el fork diferencia contra upstream debe
#      seguir siendo el del baseline. Si un fichero deja de diferenciarse, su mod se cayo.
#   2) SEMANTICA   - cadenas concretas de cada mod (comportamientos que podrian sobrevivir
#      al fichero pero revertirse por dentro).
#
# Uso:
#   bash verificar-mods.sh                      # verifica la rama actual contra su upstream
#   bash verificar-mods.sh --actualizar         # reescribe el baseline (tras migrar a V3)
#   UPSTREAM=xmage_1.4.63V2 bash verificar-mods.sh
#
# Salidas: 0 = todo presente, 1 = hay mods perdidos, 2 = error de entorno.

set -uo pipefail

# En Git Bash, un argumento "rama:ruta" se corrompe por la conversion de rutas de MSYS
# (lo reescribe como "rama;ruta" con barras invertidas) y git lo rechaza. Se desactiva
# para que `git show REF:archivo` y `git cat-file -e REF:archivo` funcionen.
export MSYS_NO_PATHCONV=1 MSYS2_ARG_CONV_EXCL='*'

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SRC="${SRC:-$HERE/XMage-1.4.62V1-src}"
BASELINE="${BASELINE:-$HERE/mods-baseline.txt}"
UPSTREAM="${UPSTREAM:-}"
UPDATE=0
[ "${1:-}" = "--actualizar" ] && UPDATE=1

cd "$SRC" 2>/dev/null || { echo "NO ENCUENTRA EL REPOSITORIO DE CODIGO: $SRC"; exit 2; }
git rev-parse --git-dir >/dev/null 2>&1 || { echo "$SRC no es un repositorio git"; exit 2; }

# REF permite verificar una rama sin cambiar el arbol de trabajo (p.ej. antes de desplegar).
REF="${REF:-$(git rev-parse --abbrev-ref HEAD)}"
if [ -z "$UPSTREAM" ]; then
    UPSTREAM="$(cat "$HERE/mods-upstream.txt" 2>/dev/null | tr -d '[:space:]')"
fi
if [ -z "$UPSTREAM" ] || ! git rev-parse -q --verify "$UPSTREAM^{commit}" >/dev/null; then
    echo "FALTA la build upstream de referencia ($UPSTREAM)."
    echo "Indicala con UPSTREAM=<tag> o escribiendola en $(basename "$HERE")/mods-upstream.txt"
    echo "Tags disponibles: $(git tag -l 'xmage_1.4.6*' | tr '\n' ' ')"
    exit 2
fi

# git diff --name-status -z emite registros NUL alternando estado y ruta; en R/C la ruta
# son dos registros (vieja, nueva), y la que existe en la rama es la nueva.
nul_lines() {
    git diff --name-status -z "$UPSTREAM" "$REF" | awk 'BEGIN{RS="\0"; st=""; mode=0}
        { if (mode==0) { st=$0; mode=(st ~ /^[RC]/) ? 2 : 1 }
          else if (mode==2) { mode=1 }
          else { printf "%s\t%s\n", st, $0; mode=0 } }' | grep -v $'\t$'
}
delta_set()   { nul_lines | cut -f2 | LC_ALL=C sort -u; }

echo "=============================================================="
echo " VERIFICACION DE MODS DEL FORK"
echo "   rama     : $REF"
echo "   upstream : $UPSTREAM ($(git log -1 --format=%h "$UPSTREAM"))"
echo "   baseline : $(basename "$BASELINE")"
echo "=============================================================="

ACTUAL="$(delta_set)"
if [ "$UPDATE" = 1 ]; then
    nul_lines | LC_ALL=C sort -k2 > "$BASELINE"
    echo "Baseline regenerado: $(wc -l < "$BASELINE") ficheros en $(basename "$BASELINE")"
    exit 0
fi
[ -f "$BASELINE" ] || { echo "No hay baseline. Ejecuta primero: bash verificar-mods.sh --actualizar"; exit 2; }

BL_PATHS="$(cut -f2 "$BASELINE" | LC_ALL=C sort -u)"

# ---------- 1) control estructural ----------
PERDIDOS="$(LC_ALL=C comm -23 <(printf '%s\n' "$BL_PATHS") <(printf '%s\n' "$ACTUAL"))"
NUEVOS="$(LC_ALL=C comm -13 <(printf '%s\n' "$BL_PATHS") <(printf '%s\n' "$ACTUAL"))"
N_PERDIDOS=$(printf '%s\n' "$PERDIDOS" | grep -c . )
N_NUEVOS=$(printf '%s\n' "$NUEVOS" | grep -c . )
BL_TOTAL=$(printf '%s\n' "$BL_PATHS" | grep -c . )
ACT_TOTAL=$(printf '%s\n' "$ACTUAL" | grep -c . )

echo
echo "--- 1) ESTRUCTURAL (ficheros que el fork diferencia de upstream) ---"
echo "    baseline: $BL_TOTAL | actual: $ACT_TOTAL | perdidos: $N_PERDIDOS | nuevos: $N_NUEVOS"
if [ "$N_PERDIDOS" -gt 0 ]; then
    echo
    echo "    !!! MOD PERDIDO (el fichero volvio a ser identico a upstream):"
    printf '%s\n' "$PERDIDOS" | sed 's/^/        /'
fi
if [ "$N_NUEVOS" -gt 0 ]; then
    echo "    (informativo) ficheros que ahora tambien difieren y no estaban en el baseline:"
    printf '%s\n' "$NUEVOS" | sed 's/^/        /'
fi

# ---------- 2) control semantico ----------
# formato: ARCHIVO<TAB>CADENA<TAB>DESCRIPCION
marcadores() {
cat <<'EOF'
Mage.Client/src/main/java/mage/client/game/FeedbackPanel.java	PREFERRED_WIDTH	mod 1-2: chat con ancho real
Mage.Client/src/main/java/mage/client/game/GamePanel.java	normalizeSplitterLocation	mod 2: divisor degenerado
Mage.Client/src/main/java/mage/client/game/GamePanel.java	Math.min(160, (int) Math.round(splitWidth * 0.10))	mod 15 (v25): arrastre del chat
Mage.Client/src/main/java/mage/client/game/GamePanel.java	PILA DE RESOLUC	mod 17 (v28): pila flotante
Mage.Client/src/main/java/mage/client/game/HelperPanel.java	getRequiredHeight	mod 7: tope del panel de ayuda
Mage.Client/src/main/java/mage/client/plugins/adapters/MageActionCallback.java	HAND_CARDS_MARGINS = new MageCardSpace(10, 10, 5, 10)	mod 4 (v19): margen de fuerza/resistencia
Mage.Client/src/main/java/org/mage/card/arcane/MageLayer.java	public static MageCardSpace getAnimationOuterSpace	mod 5 (v19): gutters compartidos
Mage.Client/src/main/java/org/mage/plugins/card/CardPluginImpl.java	getAnimationOuterSpace	mod 3 (v19): gutters dinamicos
Mage.Client/src/main/java/org/mage/plugins/card/CardPluginImpl.java	xmage.debug.bflayout	mod 13 (v26): traza de layout
Mage.Client/src/main/java/mage/client/MageFrame.java	xmage.prefs.node	mod 6 (v19): aislamiento de prefs del lab
Mage.Client/src/main/java/mage/client/game/BattlefieldPanel.java	xmage.battlefield.optimized	mod 24 (v35): opt-in de dibujo
Mage.Client/src/main/java/mage/client/chat/ChatPanelBasic.java	HH:mm:ss	mod 22 (v33): hora estable
Mage.Client/src/main/java/mage/client/components/ColorPane.java	BoxView	mod 16 (v27): repintar ante fallo del JDK
Mage.Client/src/main/java/mage/client/cards/Cards.java	PRÓXIMO	mod 17 (v28): badge LIFO
Mage.Client/src/main/java/mage/client/util/gui/Arrow.java	Gradient	mod 18 (v29): flechas cinematicas
Mage.Client/src/main/java/mage/client/dialog/GraveyardCardsDialog.java	new Color(22, 26, 35)	mod 8-19 (v30): tema oscuro cementerio
Mage.Client/src/main/java/mage/client/decks/DeckDownloaderPane.java	class DeckDownloaderPane	mod 9 (v18/v20): dashboard de decks
Mage.Server/src/main/java/mage/server/MageServerImpl.java	markGameTypesLoadFinished	mod 10 (v20): corta espera de 180 s
Mage/src/main/java/mage/cards/repository/DatabaseUtils.java	checkAndFixStaleH2Lock	mod 11 (v22): autocuracion del .lock.db
Mage.Common/src/main/java/mage/utils/MageVersion.java	-5872491576857560039	mod 12 (v23): serialVersionUID canónico
Mage.Common/src/main/java/mage/remote/MageVersionException.java	9200806506119372910	mod 12 (v23): serialVersionUID canónico
Mage.Client/src/main/java/mage/client/util/AiDeckLibrary.java	downloadedDeckRoot	mod 21 (v32): raiz de Descargados
Mage.Client/src/main/java/mage/client/deckeditor/DeckEditorPanel.java	downloadedDeckRoot	mod 21 (v32): LOAD del editor
Mage.Client/src/main/java/mage/client/util/TransformedImageCache.java	BICUBIC	mod 22 (v33): reescalado nitido
Mage.Client/src/main/java/org/mage/card/arcane/SvgUtils.java	cssDataURICache	mod 22 (v33): cache del CSS
Mage.Client/src/main/java/mage/client/game/PlayerPanelExt.java	clearLifeCache	mod 22 (v33): fuga de playerLives
Mage.Client/src/main/java/mage/client/components/ext/dlg/DialogManager.java	ConcurrentHashMap	mod 22 (v33): DCL roto
Mage.Client/src/main/java/mage/client/components/KeyBindButton.java	conflict	mod 23 (v34): conflicto de atajo se pregunta
Mage.Client/src/main/java/mage/client/tournament/TournamentPanel.java	TableRowSorter	mod 23 (v34): clasificacion ordenable
Mage.Client/src/main/java/mage/client/dialog/ConnectDialog.java	recentServers	mod 23 (v34): servidores recientes
Mage.Client/src/main/java/mage/client/util/gui/GuiDisplayUtil.java	theme-overrides.properties	mod 24 (v35): temas por fichero
Mage.Client/src/main/java/mage/client/util/SoftValuesLoadingCache.java	xmage.cache.maxEntries	mod 24 (v35): tope de caches
Mage.Client/pom.xml	flatlaf	mod 24 (v35): FlatLaf dentro del jar cliente
Mage/src/main/java/mage/cards/decks/importer/PlainTextDeckImporter.java	UTF-8	mod 25 (v36): import de .dck en UTF-8
Mage.Sets/src/mage/sets/SecretsOfStrixhaven.java	Arrays.asList()	mod 27 (v37): unfinished vacio
Mage/src/main/java/mage/abilities/effects/common/continuous/ActivateAbilitiesAnyTimeYouCouldCastInstantEffect.java	Duration duration, FilterPermanent filter	mod 28 (v38): duracion + filtro del origen
Mage/src/main/java/mage/game/permanent/Permanent.java	roomCastHalf	mod v3.4: Room no pierde la mitad del cast
Mage.Sets/src/mage/cards/s/SuperiorSpiderMan.java	setImageFileName	mod v33: fix de imagen del Spider-Man
Mage.Server.Plugins/Mage.Player.AI.MAD/src/mage/player/ai/ComputerPlayer6.java	DEEP_STACK_MAX_THINK_SECONDS	mod IA: presupuesto de simulacion profunda
Mage.Server.Plugins/Mage.Player.AI.MAD/src/mage/player/ai/ComputerPlayer6.java	transpositionTable	mod IA: tabla de transposicion
Mage/src/main/java/mage/cards/decks/CardNameUtil.java	mojibake	mod 25 (v36): Ã¶ -> o (Mjolnir)
Mage.Common/src/main/java/mage/remote/Connection.java	if (host.equals("localhost") || host.equals("127.0.0.1"))	mod v22: RMI por interfaz real
Mage.Common/src/main/java/mage/remote/interfaces/ServerState.java	Vuelve a pedir el estado al servidor	mod 10: estado incompleto al login
Mage.Client/src/main/java/mage/client/deck/generator/DeckGeneratorPool.java	allowedColorNames	mod IA: pool de mazos por color
Mage.Client/release/startClient.bat	Previous XMage client instances closed.	launchers: auto-cierre del cliente
Mage.Server/release/startServer.bat	XMAGE_SERVER_GUARD	launchers: guarda del puerto 17171
.gitignore	!Mage.Common/src/main/java/mage/db/	mod 20 (v31): mage.db versionado
EOF
}

# ficheros que deben existir tal cual (mods que son ficheros nuevos o restaurados)
presencias() {
cat <<'EOF'
Mage.Common/src/main/java/mage/db/EntityManager.java
Mage.Common/src/main/java/mage/db/EntityManagerTest.java
Mage.Common/src/main/java/mage/db/model/Feedback.java
Mage.Common/src/main/java/mage/db/model/Log.java
Mage.Sets/src/mage/cards/j/JacesMachinations.java
Mage.Tests/src/test/java/org/mage/test/cards/planeswalker/JacesMachinationsTest.java
Mage.Client/src/main/java/mage/client/cards/DragCardGrid.java
EOF
}

echo
echo "--- 2) SEMANTICO (cadenas concretas de cada mod) ---"
FALLOS=0; DEBILES=0; OKS=0
while IFS=$'\t' read -r archivo cadena desc; do
    [ -z "${archivo:-}" ] && continue
    if ! git cat-file -e "$REF:$archivo" 2>/dev/null; then
        printf '    [FALTA] %-62s %s (sin fichero)\n' "$desc" "$archivo"; FALLOS=$((FALLOS+1)); continue
    fi
    # Sin -q: grep -q corta la tuberia en cuanto acierta y, con pipefail, el SIGPIPE de
    # `git show` se cuenta como fallo (falsos negativos en ficheros grandes como GamePanel).
    if git show "$REF:$archivo" | grep -F -- "$cadena" > /dev/null 2>&1; then
        if git show "$UPSTREAM:$archivo" 2>/dev/null | grep -F -- "$cadena" > /dev/null 2>&1; then
            printf '    [debil] %-62s (tambien esta en upstream, no prueba el mod)\n' "$desc"; DEBILES=$((DEBILES+1))
        else
            OKS=$((OKS+1))
        fi
    else
        printf '    [FALTA] %-62s %s\n' "$desc" "$cadena"; FALLOS=$((FALLOS+1))
    fi
done <<< "$(marcadores)"

echo
echo "--- 3) PRESENCIA (ficheros que son el mod en si) ---"
while IFS= read -r f; do
    [ -z "$f" ] && continue
    if git cat-file -e "$REF:$f" 2>/dev/null; then
        OKS=$((OKS+1))
    else
        printf '    [FALTA] %s\n' "$f"; FALLOS=$((FALLOS+1))
    fi
done <<< "$(presencias)"

# guardas isInterrupted del fork en PlayerImpl (8 al migrar a V2)
N_GUARDAS=$(git show "$REF:Mage/src/main/java/mage/players/PlayerImpl.java" | grep -c "isInterrupted()")
if [ "${N_GUARDAS:-0}" -ge 8 ]; then
    OKS=$((OKS+1))
else
    printf '    [FALTA] PlayerImpl: %s guardas isInterrupted (se esperaban >= 8)\n' "${N_GUARDAS:-0}"; FALLOS=$((FALLOS+1))
fi

# etiquetas de version del fork: cada una debe seguir apareciendo al menos las veces conocidas
echo
echo "--- 4) AUTOETIQUETAS vNN: en los fuentes ---"
for par in 33:20 34:47 35:13 36:2 37:1; do
    v="${par%%:*}"; min="${par##*:}"
    n=$(git grep -hoE "\bv$v:" -- 'Mage.Client/src' 'Mage/src' 'Mage.Common/src' 'Mage.Server/src' 'Mage.Server.Plugins' 'Mage.Sets/src' 2>/dev/null | wc -l | tr -d ' ')
    if [ "$n" -ge "$min" ]; then
        OKS=$((OKS+1))
    else
        printf '    [FALTA] etiquetas v%s: son %s, se esperaban >= %s\n' "$v" "$n" "$min"; FALLOS=$((FALLOS+1))
    fi
done

# 5) POSICION ESTRUCTURAL. Una linea del fork puede seguir existiendo y estar MAL colocada:
# en la migracion a V2, cinco guardas isInterrupted de PlayerImpl acabaron fuera de su bucle,
# anidadas dentro de un if ajeno o duplicadas. Seguian "presentes" para el chequeo 2) y sin
# embargo no hacian nada. Aqui se compara la rama anterior del fork con la verificada: si una
# linea propia del fork aparece a la vez como + y como -, se movio de sitio.
echo
echo "--- 5) POSICION ESTRUCTURAL en los ficheros con solapamiento fork x upstream ---"
PREDECESOR="${PREDECESOR:-upgrade/official-1.4.63V1}"
SOLAPADOS='
Mage/src/main/java/mage/MageVersion.java
Mage/src/main/java/mage/utils/MageVersionException.java
Mage/src/main/java/mage/players/PlayerImpl.java
Mage/src/main/java/mage/game/permanent/Permanent.java
Mage/src/main/java/mage/game/permanent/PermanentImpl.java
Mage/src/main/java/mage/cards/RoomCard.java
Mage.Client/src/main/java/org/mage/client/cards/MageEditorPane.java
Mage.Sets/src/main/java/mage/cards/s/SuperiorSpiderMan.java
Mage.Sets/src/mage/cards/s/SuperiorSpiderMan.java
Mage.Sets/src/mage/sets/RealityFracture.java
'
PROPIAS='isInterrupted|roomCastHalf|lastBodyRule|setImageFileName|PREFERRED_WIDTH|normalizeSplitterLocation|HAND_CARDS_MARGINS|getAnimationOuterSpace|markGameTypesLoadFinished|checkAndFixStaleH2Lock|cssDataURICache|clearLifeCache|downloadedDeckRoot|DEEP_STACK|transpositionTable|RELEASE_INFO|unlockDoor'
if git rev-parse --verify -q "$PREDECESOR" >/dev/null 2>&1; then
    for archivo in $SOLAPADOS; do
        git cat-file -e "$REF:$archivo" 2>/dev/null || continue
        cuerpo() { git diff --ignore-cr-at-eol -U0 "$PREDECESOR" "$REF" -- "$archivo" \
                   | grep -vE '^(---|\+\+\+|@@|diff|index)' | grep -E "$PROPIAS"; }
        mas=$(cuerpo | grep -c '^+'); men=$(cuerpo | grep -c '^-')
        if [ "$men" -gt 0 ] && [ "$mas" -lt "$men" ]; then
            printf '    [FALTA] %s: %s lineas del fork desaparecen y solo reaparecen %s\n' "$(basename "$archivo")" "$men" "$mas"
            FALLOS=$((FALLOS+1))
        elif [ "$men" -gt 0 ]; then
            printf '    [REVISAR] %s: %s lineas del fork cambian de posicion (eran %s)\n' "$(basename "$archivo")" "$mas" "$men"
            DEBILES=$((DEBILES+1))
        else
            OKS=$((OKS+1))
        fi
    done
else
    echo "    (no existe la rama predecesora $PREDECESOR: se omite esta seccion)"
fi

echo
echo "=============================================================="
printf " RESULTADO: %d comprobaciones OK, %d FALLOS, %d marcadores debiles\n" "$OKS" "$FALLOS" "$DEBILES"
if [ "$FALLOS" -eq 0 ] && [ "$N_PERDIDOS" -eq 0 ]; then
    echo " TODOS LOS MODS PRESENTES sobre $UPSTREAM"
    echo "=============================================================="
    exit 0
fi
echo " HAY MODS SIN MIGRAR / PERDIDOS - ver lineas [FALTA] y [mod perdido] arriba"
echo "=============================================================="
exit 1
