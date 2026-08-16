# Installation

Elite Intel **V1.1** ist die aktuelle Version.

---

## <img src="images/linux.png" class="inline" height="20" alt="Linux"> Linux <img src="images/windows.png" class="inline" height="20" alt="Windows"> Windows

1. Den [👉**Installer**👈](https://github.com/stone-alex/EliteIntel/releases) herunterladen.
2. Den Installer ausführen und den Anweisungen auf dem Bildschirm folgen.
3. Ein LLM einrichten. **Ohne LLM läuft Elite Intel nicht.** Zwei Optionen stehen zur Verfügung:
   - **Lokales LLM** (kostenlos, offline): Siehe die [**Lokale LLM-Anleitung**](installing-local-llms).
     Erfordert leistungsfähige GPU-Hardware.
   - **Cloud-LLM** (hat eine kostenlose Option und ist einfacher einzurichten): Siehe
     [**Cloud-LLM-Optionen**](cloud-llm-options), um einen API-Schlüssel zu bekommen, und trage ihn
     dann unter [**Einstellungen → KI-Dienste**](UI-Settings-Tab) ein.
     Kostenloser Tarif, ohne Kreditkarte: 👉 [**console.mistral.ai**](https://console.mistral.ai/) 👈

Setup abgeschlossen. Weiter mit [**der Benutzeroberfläche, Reiter für Reiter**](UI).

### Checkliste für den ersten Start

Elite Intel spricht diese Warnungen beim Start der Dienste laut aus, du erfährst also von allem,
was fehlt — es lohnt sich aber, das vorab zu erledigen:

| Schritt | Wo |
|------|-------|
| Auf ein Sprachmodell verweisen | [Einstellungen → KI-Dienste](UI-Settings-Tab) |
| Journal-Ordner prüfen | [Einstellungen → Allgemein](UI-Settings-Tab) |
| Belegungsordner prüfen und fehlende Belegungen beheben | [Reiter Bindings](UI-Bindings-Tab) |
| Audio kalibrieren | [Vega-Reiter](UI-Vega-Tab) → **AUDIO KALIBRIEREN** |

---

### Deinstallation (Linux)

```shell
~/.var/app/elite.intel.app/uninstall
```

----
Bei Problemen bitte auf Matrix melden. Fehlermeldungen und Pull Requests sind willkommen.

Community 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈