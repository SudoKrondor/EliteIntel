# Elite Intel
## KI-Sprachassistent für Elite Dangerous

![EliteIntel app](images/V1.2.png)


## Natürlich sprechen. Besser fliegen.

Elite Intel verbindet deine Stimme mit deinem Schiff. Keine Befehlslisten zum Auswendiglernen, keine starre Syntax zu erlernen. Sag einfach, was du meinst, und die KI erledigt den Rest. Ob du Schiffssysteme verwaltest, eine Route planst oder ein System nach Signalen abscannst – Elite Intel versteht deine Absicht und handelt entsprechend.

Daten werden in Echtzeit von [Spansh](https://spansh.co.uk/) und [EDSM](https://www.edsm.net/) bezogen.

**Unterstützte Sprachen:** Englisch, Russisch, Ukrainisch, Deutsch, Französisch, Spanisch, Italienisch, Portugiesisch, brasilianisches Portugiesisch

> ### ⚠️ Ein Sprachmodell (LLM) wird benötigt
> Elite Intel ist Stimme und Hände — das LLM ist das Gehirn. Ohne LLM läuft die App nicht.
> Betreibe ein Modell lokal auf deiner GPU oder nutze einen Cloud-Anbieter mit deinem eigenen API-Schlüssel.
> Siehe [Cloud-LLM](cloud-llm-options) oder [LLM auswählen](installing-local-llms).

---

## Intelligenz, nicht nur Automatisierung

Elite Intel drückt nicht einfach nur Tasten für dich. Das Sprachmodell wendet Fachwissen an, um dir aussagekräftige Analysen zu liefern. Bitte es, deine aktuelle Ausrüstung zu bewerten, und es wird Stärken, Schwächen und Kompromisse aufzeigen – und keine Spezifikationstabelle vorlesen.

Sitzungsdaten aus automatischen Scans und FSS-Ergebnissen fließen direkt in Abfragen wie *„Was zeigen die Scanner?"* oder *„Lokale Signale analysieren."* ein. Die KI arbeitet mit dem, was das Spiel preisgibt. Wenn Daten nicht verfügbar sind – weil sie nur auf einem Panel und nicht im Journal existieren – wird sie das mitteilen.

---

## Dein Schiff. Deine Hardware. Deine Regeln.

Elite Intel läuft vollständig auf deinem Rechner. Die gesamte Pipeline (Spracherkennung, Sprachmodell und Sprachsynthese) arbeitet offline. Kein Cloud-Konto, kein Abonnement, keine Daten verlassen dein System.

Bevorzugst du ein hybrides Setup? Kombiniere frei nach Belieben: lokale Spracherkennung mit einem Cloud-Sprachmodell oder Cloud-Sprachsynthese mit lokaler Inferenz. Die Entscheidung liegt bei dir.

---

## Spracherkennung, die wirklich funktioniert

Elite Intel verwendet das **NVIDIA Parakeet** Transducer-Modell für die Sprache-zu-Text-Umwandlung – dieselbe Technologieklasse, die in professionellen Transkriptionswerkzeugen eingesetzt wird. Es läuft sowohl unter Windows als auch Linux ohne zusätzliches Setup oder Training.

Wenn du bereits Spracherkennungssoftware mit Elite Dangerous verwendet hast, wird ein deutlicher Unterschied in der Genauigkeit auffallen.

---

## Eine Stimme, die zum Cockpit passt

**Supertonic 3** ist integriert und funktioniert sofort. Keine Downloads, keine API-Schlüssel, keine Konfiguration, und nichts verlässt deinen PC. Es bietet 45 für Elite Intel erstellte Stimmen, und jede Stimme spricht jede von Elite Intel unterstützte Sprache. Bei russischer Sprache setzt die Bibliothek RUAccent die Betonung, sodass Wörter, deren Bedeutung davon abhängt, richtig ausgesprochen werden. Du kannst [jede Stimme abstimmen](UI-Settings-Tab), bis sie dir gefällt.

Lieber eine Cloud-Stimme? **Google TTS** steht als Option bereit. Dafür ist ein API-Schlüssel nötig, den du unter [Dienste → KI-Dienste](UI-Settings-Tab) einträgst.

---


## Deine Steuerung, geschützt

**[BindForge](UI-Bindings-Tab)** bearbeitet deine Steuerung für Elite Dangerous von außerhalb des Spiels: Tastatur, Maus, Joystick, HOTAS und Pedale. Lege jedes Gerät auf jeden Slot, behebe Konflikte, stimme Empfindlichkeiten und Totzonen ab und gib deinen Controllern echte Namen und Tastenbeschriftungen.

Eine aufwendige Belegung kostet Stunden, und ein Spiel-Update kann sie zurücksetzen. BindForge sichert deine Steuerungsdateien jeden Tag und vor jeder Änderung und kann sie zurückspielen, wenn du sie brauchst.

---

## Wähle dein LLM

Elite Intel funktioniert mit dem Sprachmodell, das zu deinem Setup passt:

**Kostenlos und offline** - [LM Studio](https://lmstudio.ai/) mit einem lokalen Modell  
**Kostenpflichtige Cloud** - Anthropic, OpenAI, xAI, Gemini, DeepSeek

Wechsle den Anbieter jederzeit im Reiter Dienste. Keine Neuinstallation erforderlich.

---

## Community

Elite Intel ist Open Source. Fehlermeldungen, Feature-Anfragen und Pull Requests sind willkommen.

Tritt der Community auf Matrix bei: 👉 [**#krondor:matrix.org**](https://app.element.io/#/room/#krondor:matrix.org) 👈

Noch nie Matrix benutzt? 👉 [**Matrix-Anleitung**](../Matrix-How-To.html) (Englisch): Schritt für Schritt zum Konto und in den Raum.

---

*Behalte die manuelle Kontrolle über kritische Schiffssysteme jederzeit.*
