# NpcMenu (Paper 26.x)

NPCs per Menue erstellen. Die NPCs sind echte **Mannequin**-Entities (Skin per Spielername).

## Bauen
Voraussetzung: JDK 25 (Paper 26.x laeuft auf Java 25).

1. In `build.gradle.kts` die Zeile `paperVersion` an deine Paper-Version anpassen.
2. `./gradlew build` (oder `gradle build`)
3. `build/libs/NpcMenu-1.0.0.jar` in den `plugins/`-Ordner kopieren.

## Benutzung
- `/npc` -> Verwaltung (Permission: `npcmenu.admin`)
- "+ Neuer NPC" -> Skin, Name, Typ, Einstellungen -> "NPC spawnen"
- **Shift + Rechtsklick** auf einen NPC = bearbeiten (nur Admins)
- Rechtsklick = Aktion des NPCs

## Typen
- Shop/Trader: bis zu 9 Tausche (z.B. 8 Fackeln -> 1 Lichtblock)
- Teleporter: eigene Position oder Weltspawn einer Welt
- Befehl: als Spieler oder Konsole, `%player%` wird ersetzt
- Nachricht: Text mit MiniMessage oder &-Codes
- Server wechseln: Proxy (BungeeCord/Velocity), Servername aus der Proxy-Config

## Ohne lokales Java bauen (GitHub)
1. Neues GitHub-Repository anlegen und den Inhalt dieses Ordners hochladen (inkl. `.github`).
2. Reiter "Actions" -> Workflow "Build" laeuft automatisch.
3. Fertige JAR unter "Artifacts" -> "NpcMenu" herunterladen.
