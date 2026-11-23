# Installation

Elite Intel **V1.2** ist die aktuelle Version.

---

## <img src="images/linux.png" class="inline" height="20" alt="Linux"> Linux <img src="images/windows.png" class="inline" height="20" alt="Windows"> Windows

1. Den [👉**Installer**👈](https://github.com/stone-alex/EliteIntel/releases) herunterladen.
2. Den Installer ausführen und den Anweisungen auf dem Bildschirm folgen.
3. Ein LLM einrichten. **Ohne LLM läuft Elite Intel nicht.** Zwei Optionen stehen zur Verfügung:
   - **Lokales LLM** (kostenlos, offline): Siehe die [**Lokale LLM-Anleitung**](installing-local-llms).
     Erfordert leistungsfähige GPU-Hardware.
   - **Cloud-LLM** (kostenpflichtig und einfacher einzurichten): Siehe
     [**Cloud-LLM-Optionen**](cloud-llm-options), um einen API-Schlüssel zu bekommen, und trage ihn
     dann unter [**Dienste → KI-Dienste**](UI-Settings-Tab) ein.

Setup abgeschlossen. Weiter mit [**der Benutzeroberfläche, Reiter für Reiter**](UI).

### Checkliste für den ersten Start

Elite Intel spricht diese Warnungen beim Start der Dienste laut aus, du erfährst also von allem,
was fehlt — es lohnt sich aber, das vorab zu erledigen:

| Schritt | Wo |
|------|-------|
| Auf ein Sprachmodell verweisen | [Dienste → KI-Dienste](UI-Settings-Tab) |
| Journal-Ordner prüfen | [Reiter Kommandant](UI-Commander-Tab) |
| BindForge einrichten lassen und Vega die nötigen Tasten geben | [Reiter BindForge](UI-Bindings-Tab) |
| Audio kalibrieren | [Vega-Reiter](UI-Vega-Tab) → **AUDIO KALIBRIEREN** |

---

## Was Elite Intel für dich einrichtet

Drei Dinge, die du bei einer frischen Installation vorab kennen solltest.

### LM Studio, für dich installiert

Nutzt du LM Studio nur für Elite Intel, **lass Elite Intel es installieren. Das ist der empfohlene
Weg.** Drücke **LM Studio installieren** unter [Dienste → KI-Dienste](UI-Settings-Tab) →
**LMStudio**. Elite Intel installiert LM Studio, lädt das Modell herunter und lädt es, testet es und
trägt **Adresse** und **Modell** für dich ein. Danach startet es LM Studio mit seinen Diensten und
beendet es, wenn du sie stoppst oder die App schließt – das Modell belegt den Speicher deiner
Grafikkarte also nur, solange Elite Intel läuft. **LM Studio deinstallieren** entfernt es wieder.

Die Schaltfläche erscheint nur, wenn deine Grafikkarten Spiel und Modell zusammen fassen: eine NVIDIA
RTX 3090 mit 24 GB oder besser, zwei neuere NVIDIA-Karten mit zusammen etwa 20 GB oder AMD-Karten mit
gleich viel Speicher, als Beta. Details stehen in der [Lokale LLM-Anleitung](installing-local-llms).
Nutzt du LM Studio auch für anderes, installiere es selbst: Elite Intel fügt dann nur das Modell hinzu
und füllt die Felder aus, und startet, stoppt oder entfernt dein LM Studio nie.

### Dein HOTAS, deine Joysticks und Pedale

Wenn du den [Reiter BindForge](UI-Bindings-Tab) zum ersten Mal öffnest, sichert er deine
Steuerungsdateien, findet jede Kopie des Spiels auf diesem PC und schaut, was angeschlossen ist. Ab
Werk kennt das Spiel viele Controller nur über eine Hardware-ID. BindForge bietet jedem Controller
ohne Namen einen echten an, etwa *Throttle* oder *Pedals*, und du kannst das **Überspringen**.

![Benannte Controller im Alias-Designer](images/ui-bindings-alias-designer.png)

Später drückst du im [Alias-Designer](UI-Bindings-Tab#alias-designer) das **Zahnrad** eines
Controllers, um seine Tasten, Achsen und Hats zu beschriften. Drückst du eine Taste, leuchtet ihr Feld
auf – so siehst du, welche welche ist. Die Beschriftungen erscheinen im Steuerungsbildschirm des Spiels
und im Bindungseditor.

![Tastenbeschriftungen im Geräte-Editor](images/ui-bindings-device-editor.png)

### Das Spiel aus mehr als einem Shop

Du hast Elite Dangerous bei Steam und bei Epic? Alle Kopien teilen sich eine Steuerung, aber jede führt
ihre eigene Liste deiner Controller und Tastennamen. Weichen die Listen voneinander ab, kann deine
Steuerung in der einen Kopie funktionieren und in der anderen auf Tastatur und Maus zurückfallen.
BindForge erkennt das beim ersten Öffnen und bietet **Spielkopien angleichen** an:

![Spielkopien angleichen](images/install-reconcile-welcome.png)

1. **Anschließen** – schließe die Controller an, mit denen du fliegst. Drückst du eine Taste,
   leuchtet ihre Zeile auf.
2. **Wählen** – beantworte ein paar kurze Fragen, etwa welchen Namen ein Controller behalten soll, den
   jede Kopie anders nennt. Deine Steuerung folgt dem gewählten Namen. Zu viele Fragen? **Alles wie in
   einer meiner Kopien einrichten...** richtet alles so ein wie in der Kopie, die du am meisten spielst.
3. **Prüfen** – sieh genau, was jede Kopie bekommt und warum, bevor sich etwas ändert.

![Ergebnis prüfen](images/install-reconcile-check.png)

**Bis zum letzten Schritt ändert sich nichts, und vorher wird alles gesichert.** **Nicht jetzt** lässt
deine Dateien unangetastet. Ändern sich später die Steuerungsdateien einer Kopie, etwa nach einem
Spiel-Update, fragt BindForge nur **Welche Einrichtung ist aktuell?** und gleicht die anderen an.

---

### Neu in Elite Dangerous?

Das Spiel legt seine Steuerungsdateien erst an, wenn du etwas geändert hast, und ohne sie kann Elite Intel
dein Schiff nicht bedienen. Schließe dein Kommandantentraining ab und stelle dann im Spiel unter *Optionen →
Steuerung* die UI-Bewegung der Galaxiekarte auf die Pfeiltasten und übernimm das.

### Umzug auf einen anderen Rechner

[**Einstellungen & Daten sichern**](UI-Settings-Tab) im Reiter Dienste legt deine Einstellungen, deine Daten
und deine eigenen Befehle in einer Datei ab, die du auf dem neuen Rechner wiederherstellen kannst.

---

### Deinstallation (Linux)

```shell
~/.var/app/elite.intel.app/uninstall
```

----
Bei Problemen nutze **Fehler melden** im Reiter Vega: Deine Beschreibung geht direkt an das Team, ganz ohne Konto. Zum Chatten mit uns komm mit einem kostenlosen Konto in die Community auf Matrix. Am besten mit der App Element, für einen kurzen Chat reicht aber jeder aktuelle Browser. Fehlermeldungen und Pull Requests sind willkommen.

Community 👉[**Matrix**](https://app.element.io/#/room/#krondor:matrix.org)👈