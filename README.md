# HUGOTiers Tagger

Fabric client mod that shows a player's HTier rank (LT5 – HT1) as a coloured badge right next
to their nametag — for the [HUGOTiers](https://hugotiers.net) leaderboard.

*(Deutsche Fassung weiter unten.)*

## What it does

- Draws the rank of every player next to their nametag, e.g. `⛏ HT1 Player`.
- Kit icon in front of the rank: Cave · Crystal · Mace.
- Colours exactly as on the website (Tier 1 = gold … Tier 5 = grey, HT brighter than LT).
- Optional animated rainbow outline around the badge.
- Selectable mode: all tested kits, or a single kit.

## Network & privacy

The mod **never** talks to a database directly. It reads exactly one thing: the **public,
read-only JSON API** at `https://hugotiers.net/api/v1`. That API is the security boundary.

What is transmitted:

- The **UUIDs of the players currently visible to you**, so their ranks can be looked up.
- Nothing else — no chat, no position, no account data, no telemetry, no analytics.

Properties of that design:

- **No credentials in the jar.** There is nothing to extract.
- **Read-only, cached snapshot.** The API cannot modify the backend.
- **Backend-friendly.** Requests are batched (one request per tab list, up to 200 players),
  at most once per second, and results are cached client-side (5 minutes by default).
  That stays far below the API's rate limit.
- **UUID-based.** Lookups use the Minecraft UUID, not the name — a name change can therefore
  never produce a wrong or missing rank.
- **Configurable.** `apiBaseUrl` in `config/hugotiers-tagger.json` points the mod at a
  different instance; deleting the mod stops all requests.

## No abuse (nametag visibility)

The badge is injected into the **exact vanilla nametag render path**. It only appears when
Minecraft draws the nametag anyway. If a player is invisible, sneaking, out of range, if the
nametag display is off via F1/options, or if team rules hide the name — then **no rank shows
either**. There is no way to see more through this mod than vanilla intends.

## No wrong ranks

Only what the API actually returns is displayed. Unknown or new rank codes are discarded
rather than guessed, "not found" shows nothing, and stale entries expire after the cache TTL
and are re-fetched.

## Requirements

- Minecraft **1.21.11**
- **Fabric Loader** ≥ 0.16
- **Fabric API**
- **Java 21**

## Installation

1. Install Fabric Loader and Fabric API.
2. Drop `hugotiers-tagger-1.1.0.jar` into your `mods/` folder.
3. Start Minecraft (client).

## Configuration

Everything lives in an in-game settings screen. Three ways to open it:

- **ModMenu** → the mod's entry → settings button. ModMenu is an *optional* dependency; the mod
  works without it.
- **Options › Controls › Key Binds** → bind "Open settings". Unbound by default, so it never
  steals a key from you.
- The client command **`/hugotiers`**.

| Setting | Effect |
|---|---|
| Show tier badges | Badge display on/off |
| Rainbow outline | Animated colour-cycling outline around the badge |
| Kits shown | `All kits`, or a single kit: Cave, Crystal, Mace |
| Refresh interval | How long a looked-up rank is reused before it is fetched again |
| API address | Which host the ranks are read from |

Note: picking a single kit shows **only** that kit — players without a rank in it get no badge.
`All kits` is the default.

Persisted in `config/hugotiers-tagger.json`:

| Key | Default | Meaning |
|---|---|---|
| `enabled` | `true` | Badge display on/off |
| `gamemode` | `"all"` | Display mode (see above) |
| `rainbow` | `true` | Animated outline |
| `apiBaseUrl` | `"https://hugotiers.net"` | API host |
| `cacheTtlSeconds` | `300` | Cache lifetime per player |

## Building

```bash
./gradlew build
```

Result: `build/libs/hugotiers-tagger-1.1.0.jar`.

## Licence & credits

[MIT](LICENSE) — open source and free to inspect.

Bundled font for the tier labels: **Montserrat**, © 2024 The Montserrat Project Authors,
licensed under the SIL Open Font License 1.1 — full licence text in [OFL.txt](OFL.txt).

---

# HUGOTiers Tagger (Deutsch)

Fabric-Client-Mod, die den HTier-Rang eines Spielers (LT5 – HT1) als farbige Plakette direkt
neben dem Nametag anzeigt — für die [HUGOTiers](https://hugotiers.net)-Rangliste.

## Was sie tut

- Zeigt neben jedem Spieler-Nametag dessen Rang, z. B. `⛏ HT1 Spieler`.
- Kit-Icon vor dem Rang: Cave · Crystal · Mace.
- Farben exakt wie auf der Website (Tier 1 = Gold … Tier 5 = Grau, HT heller als LT).
- Optionaler animierter Rainbow-Rahmen um die Plakette.
- Wählbarer Modus: alle getesteten Kits oder ein einzelnes Kit.

## Netzwerk & Datenschutz

Die Mod spricht **niemals** direkt mit einer Datenbank. Sie liest genau eine Sache: die
**öffentliche, read-only JSON-API** unter `https://hugotiers.net/api/v1`. Diese API ist die
Sicherheitsgrenze.

Was übertragen wird:

- Die **UUIDs der Spieler, die du gerade siehst**, um deren Ränge nachzuschlagen.
- Sonst nichts — kein Chat, keine Position, keine Account-Daten, keine Telemetrie.

Eigenschaften dieses Designs:

- **Keine Zugangsdaten im Jar.** Es gibt nichts zu extrahieren.
- **Nur lesend, nur ein gecachter Snapshot.** Die API kann das Backend nicht verändern.
- **Backend-schonend.** Anfragen werden gebündelt (eine pro Tab-Liste, bis zu 200 Spieler),
  höchstens einmal pro Sekunde, Ergebnisse werden clientseitig zwischengespeichert
  (Standard 5 Minuten). Das bleibt weit unter dem Rate-Limit der API.
- **UUID-basiert.** Nachgeschlagen wird über die Minecraft-UUID, nicht den Namen — ein
  Namenswechsel führt also nie zu einem falschen oder fehlenden Rang.
- **Konfigurierbar.** `apiBaseUrl` in `config/hugotiers-tagger.json` zeigt auf eine andere
  Instanz; die Mod zu löschen stoppt alle Anfragen.

## Kein Missbrauch (Nametag-Sichtbarkeit)

Die Plakette wird **exakt im Vanilla-Nametag-Renderpfad** eingehängt. Sie erscheint nur dann,
wenn Minecraft den Nametag ohnehin zeichnet. Ist ein Spieler unsichtbar, sneakt, ausser
Reichweite, ist die Nametag-Anzeige per F1/Optionen aus, oder verstecken Team-Regeln den Namen
— dann erscheint **auch kein Rang**. Es gibt keinen Weg, über die Mod mehr zu sehen als von
Vanilla vorgesehen.

## Keine falschen Ränge

Angezeigt wird nur, was die API tatsächlich liefert. Unbekannte oder neue Rang-Codes werden
verworfen statt geraten, „nicht gefunden" zeigt nichts an, und veraltete Einträge laufen nach
der Cache-Zeit ab und werden neu geladen.

## Voraussetzungen

- Minecraft **1.21.11**
- **Fabric Loader** ≥ 0.16
- **Fabric API**
- **Java 21**

## Installation

1. Fabric Loader und Fabric API installieren.
2. `hugotiers-tagger-1.1.0.jar` in den `mods/`-Ordner legen.
3. Minecraft (Client) starten.

## Konfiguration

Alles liegt in einem In-Game-Einstellungsfenster. Drei Wege dorthin:

- **ModMenu** → Eintrag der Mod → Einstellungen-Button. ModMenu ist eine *optionale* Abhängigkeit;
  die Mod läuft auch ohne.
- **Optionen › Steuerung › Tastenbelegung** → „Einstellungen öffnen" belegen. Standardmässig
  unbelegt, nimmt dir also nie eine Taste weg.
- Der Client-Befehl **`/hugotiers`**.

| Einstellung | Wirkung |
|---|---|
| Tier-Plaketten anzeigen | Anzeige an/aus |
| Rainbow-Rahmen | Animierter, farbwechselnder Rahmen um die Plakette |
| Angezeigte Kits | `Alle Kits` oder ein einzelnes: Cave, Crystal, Mace |
| Aktualisierung | Wie lange ein abgefragter Rang wiederverwendet wird |
| API-Adresse | Von welchem Host die Ränge gelesen werden |

Hinweis: ein einzelnes Kit zeigt **nur** dieses — Spieler ohne Rang darin bekommen keine Plakette.
Standard ist `Alle Kits`.

Persistiert in `config/hugotiers-tagger.json`:

| Schlüssel | Standard | Bedeutung |
|---|---|---|
| `enabled` | `true` | Anzeige an/aus |
| `gamemode` | `"all"` | Anzeigemodus (siehe oben) |
| `rainbow` | `true` | Animierter Rahmen |
| `apiBaseUrl` | `"https://hugotiers.net"` | API-Host |
| `cacheTtlSeconds` | `300` | Cache-Lebensdauer pro Spieler |

## Bauen

```bash
./gradlew build
```

Ergebnis: `build/libs/hugotiers-tagger-1.1.0.jar`.

## Lizenz & Credits

[MIT](LICENSE) — quelloffen und frei einsehbar.

Gebündelte Schrift für die Tier-Kürzel: **Montserrat**, © 2024 The Montserrat Project Authors,
lizenziert unter der SIL Open Font License 1.1 — voller Lizenztext in [OFL.txt](OFL.txt).
