# HUGOTiers Tagger

Shows each player's **HTier rank** (LT5–HT1) right next to their nametag — with a kit icon,
in the original tier colours, and an optional animated **rainbow outline**.
For the [HUGOTiers](https://hugotiers.net) leaderboard.

## Features
- 🏷️ Rank + kit icon (Cave · Crystal · Mace) on the nametag
- 🎨 Original tier colours from the website
- 🌈 Optional animated rainbow outline
- ⚡ Loads instantly, cached, FPS-friendly
- 🔒 UUID-based (survives name changes) and respects nametag visibility — no abuse

## Settings
Everything is configured in an in-game settings screen — badge on/off, rainbow outline, which kits
are shown, refresh interval, API address. Open it in any of three ways:

- **ModMenu** → the mod's entry → settings button (ModMenu is optional, not required)
- **Options › Controls › Key Binds** → bind "Open settings" (unbound by default, so it steals no key)
- the command **`/hugotiers`**

## Requirements
Minecraft 1.21.11 · Fabric Loader · Fabric API · Java 21 · ModMenu optional

## Network & privacy
This mod talks to one server: **hugotiers.net**.

- It sends the **UUIDs of the players you can currently see** (batched, at most once per second)
  to the public endpoint `https://hugotiers.net/api/v1` in order to look up their ranks.
- Nothing else leaves your client — no chat, no position, no account data, no telemetry.
- The endpoint is **public and read-only**; the mod has no database access and no credentials.
- The server address is configurable in `config/hugotiers-tagger.json` — set it to your own
  instance or delete the mod if you do not want these requests.

Open source (MIT): https://github.com/Fullydefense/hugotiers-tagger
Bundled font: Montserrat (SIL Open Font License 1.1).

---

# HUGOTiers Tagger (Deutsch)

Zeigt den **HTier-Rang** (LT5–HT1) jedes Spielers direkt am Nametag — mit Kit-Icon, in den
echten Tier-Farben und optionalem animiertem **Rainbow-Rahmen**.
Für die [HUGOTiers](https://hugotiers.net)-Rangliste.

## Features
- 🏷️ Rang + Kit-Icon (Cave · Crystal · Mace) am Nametag
- 🎨 Original-Tier-Farben von der Website
- 🌈 Optionaler animierter Rainbow-Rahmen
- ⚡ Lädt sofort, gecacht, FPS-freundlich
- 🔒 UUID-basiert (namenswechsel-sicher) & respektiert die Nametag-Sichtbarkeit (kein Abuse)

## Einstellungen
Alles wird in einem In-Game-Einstellungsfenster konfiguriert — Plakette an/aus, Rainbow-Rahmen,
angezeigte Kits, Aktualisierungsintervall, API-Adresse. Drei Wege dorthin:

- **ModMenu** → Eintrag der Mod → Einstellungen-Button (ModMenu ist optional, nicht nötig)
- **Optionen › Steuerung › Tastenbelegung** → „Einstellungen öffnen" belegen (standardmässig
  unbelegt, nimmt dir also keine Taste weg)
- der Befehl **`/hugotiers`**

## Voraussetzungen
Minecraft 1.21.11 · Fabric Loader · Fabric API · Java 21 · ModMenu optional

## Netzwerk & Datenschutz
Die Mod spricht mit genau einem Server: **hugotiers.net**.

- Sie sendet die **UUIDs der Spieler, die du gerade siehst** (gebündelt, höchstens einmal pro
  Sekunde) an den öffentlichen Endpunkt `https://hugotiers.net/api/v1`, um deren Ränge abzufragen.
- Sonst verlässt nichts deinen Client — kein Chat, keine Position, keine Account-Daten,
  keine Telemetrie.
- Der Endpunkt ist **öffentlich und read-only**; die Mod hat keinen Datenbankzugriff und
  keine Zugangsdaten.
- Die Server-Adresse ist in `config/hugotiers-tagger.json` einstellbar — trag deine eigene
  Instanz ein oder lösche die Mod, wenn du diese Anfragen nicht willst.

Open Source (MIT): https://github.com/Fullydefense/hugotiers-tagger
Gebündelte Schrift: Montserrat (SIL Open Font License 1.1).
