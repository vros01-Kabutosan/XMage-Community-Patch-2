# Taste
- Test and validate changes in the laboratory (sandbox) first; promote to the local installation only when everything is stable and tests pass. Confidence: 0.9
- If a fix cannot be made, revert to the last known-good working state rather than leave broken changes in place; restore original paths/names and remove temporary shims or added folders. Confidence: 0.9
- After activating changes locally, sync with git and clean/order the working tree so the project stays tidy for continued work. Confidence: 0.9
- Keep the MTG root minimal: only the personal installation folder plus one aggregate folder (e.g. Laboratorio) for everything else; move non-installation content into that aggregate folder. Confidence: 0.85
- For the MTG/XMage project, keep all project files under `J:\\MTG\\Instalacion-XMage`; unrelated directories (e.g. DD2) should only be used as junctions to satisfy hardcoded client paths, not as storage, and any visible references to them should be cleaned from project files/logs. Confidence: 0.95
- Respond in Spanish (user communicates in Spanish). Confidence: 0.9
