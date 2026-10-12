# Gamba Slot Machine (Fabric 1.21.11)

Ein Fabric-Mod-Projekt für Minecraft Java Edition 1.21.11. Es stellt eine Slotmaschine in einem normalen, sechzeiligen Truhen-GUI mit blauem Akzent bereit.

## Features

- Blaues Chest-GUI mit sauber getrennten Buttons und Info-Slots.
- Einsatzvalidierung: 0 und negative Einsätze werden im Chat abgewiesen; nach ungültiger Eingabe bleibt der Dreh blockiert, bis ein gültiger Einsatz gesetzt wurde.
- Einsatz in Emeralds, Standard-Einsatz 1, GUI-Limit 1–64 Emeralds.
- Zufällige Animation zwischen 40 und 80 Ticks (2–4 Sekunden).
- Ore-Symbole: Coal Ore 0.5x, Iron Ore 1.2x, Gold Ore 2.0x, Diamond Ore 5.0x, Emerald Ore 10.0x, Ancient Debris / Netherite 25.0x.
- Gewinn nur bei drei gleichen Symbolen in der mittleren Reihe.
- Während eines laufenden Drehs ist ein weiterer Dreh blockiert.
- Alle Abbuchungen, Validierungen, Ergebnisse und Auszahlungen werden serverseitig verarbeitet.

## Befehle

- `/gamba` – öffnet die Slotmaschine.
- `/gamba bet <zahl>` – setzt den Einsatz. Beispiel: `/gamba bet 10`.
- `/gamba balance` – zeigt die Emeralds im Inventar an, die als Einsatz genutzt werden können.
- `/gamba odds` – zeigt alle Multiplikatoren im Chat.

Es zählen Emeralds in Hotbar und Hauptinventar, nicht in Rüstung oder Offhand. Der Einsatz wird beim Start abgebucht. Bei drei gleichen Symbolen wird `Einsatz × Multiplikator` als Emeralds ausgezahlt; Bruchteile werden abgerundet. Ohne drei gleiche Symbole gibt es keine Auszahlung.

## Build (Windows)

1. Installiere Java 21.
2. Öffne diesen Ordner in IntelliJ IDEA oder öffne darin ein Terminal.
3. Baue die Mod mit:
   ```powershell
   .\gradlew.bat build
   ```
4. Die Mod-JAR liegt anschließend in `build\libs\gamba-mod-1.0.0.jar` (die genaue Datei kann je nach Build-Ausgabe einen zusätzlichen Klassifikator haben).
5. Installiere Fabric Loader für Minecraft 1.21.11. Lege die JAR zusammen mit Fabric API in den `mods`-Ordner.

## Build (Linux/macOS)

```bash
chmod +x ./gradlew
./gradlew build
```

## GitHub

Erstelle ein leeres Repository auf GitHub, entpacke dieses Projekt, und lade den gesamten Inhalt inklusive `src`, `build.gradle`, `settings.gradle`, `gradle.properties` und Gradle Wrapper hoch. Nicht den Ordner `build/` committen.

## Versions

- Minecraft: 1.21.11
- Java: 21
- Fabric Loader: 0.19.5
- Fabric API: 0.141.6+1.21.11
- Yarn Mappings: 1.21.11+build.4

## Hinweis

Diese Mod verwendet ausschließlich Emeralds als virtuelle Ingame-Einsatzwährung; es gibt keine Echtgeldfunktion.

Die in diesem Projekt enthaltenen `gradlew`-Skripte laden beim ersten Start Gradle 9.7.1 herunter. Dafür ist eine Internetverbindung erforderlich. In dieser Arbeitsumgebung konnte der eigentliche Minecraft-Build nicht ausgeführt werden, weil der Zugriff auf die externen Download-Hosts nicht verfügbar war; das Projekt ist daher als Quellprojekt, nicht als bereits getestete JAR, bereitgestellt.
