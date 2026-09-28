# minecraft-tiered-trinkets — instructions agents

Mod Fabric 1.21.1, serveur seul (joueurs : rien, noms de palier via le pack de ressources du serveur). Serveur
`Nistroy/minecraft-server` (`MODS.md`). Public, GPL-3.0. Docs `.md` = notes denses pour agents, sauf `README.md`.

## But (demande nistroy 2026-09-28)
1. Palier TieredZ d'un objet porté dans un emplacement Trinkets → ses bonus s'appliquent (TieredZ : emplacements
   vanilla seulement).
2. Paliers pour bijoux (Jewelry), sacs à dos (Traveler's Backpack), Élytre des âmes (Deeper and Darker).
   Exclus (choix nistroy) : clé, carquois, charme, emplacement pêche de Tide.

## Carte
- `mixin/TrinketModifiersMixin` — `@ModifyReturnValue` sur `dev.emi.trinkets.TrinketModifiers.get*` (2 surcharges).
  Trinkets appelle `get(stack, ref, entity)` à l'équipement (ajout) et au déséquipement (retrait) dans son
  `LivingEntityMixin.tick` ; `lastEquippedTrinkets` non sauvegardé → réappliqué à la connexion.
- `TieredTrinkets` (main) — charge `TrinketModifiers` au démarrage : mixin cassé → le serveur plante au lancement,
  pas à la 1re connexion. Vérifié sur `test-server` 2026-09-28 (`Loaded 342 tiers`, démarrage OK).
- `TrinketTierModifiers` — gabarits du palier listant `CHEST` ou `BODY` seulement ; id `tieredtrinkets:<slot>/<i>`
  (`SlotAttributes.getIdentifier`) → deux anneaux ne s'écrasent pas.
- `tools/generate_tiers.py <tiered.jar>` → `src/main/resources/data/tiered/` + `lang/en_us.json`. Données
  générées, ne pas éditer à la main.
- `lang/en_us.json` — `<id>.label` (nom du palier dans le nom de l'objet, côté client) → à copier dans le pack
  `enchants-plus-lang` du serveur (`assets/tiered/lang/en_us.json`, fusion avec les clés des bâtons de mage).

## Données (thèmes validés par nistroy 2026-09-28)
- Bijoux et sacs : gabarits en `BODY` (emplacement jamais porté par un joueur) → pas appliqués en main par TieredZ,
  appliqués par ce mod. Infobulle TieredZ côté client : « Sur le corps ». Élytre : gabarits `CHEST` d'origine.
- 1 fichier par palier et par thème ; poids TieredZ 50/35/15/8/3/0 → mêmes chances que 4 variantes par palier.
- Bijoux = moitié des armes : thème en `ADD_MULTIPLIED_TOTAL` −15/−5/+5/+5/+10/+15 %, secondaire dès Épique.
  Thèmes = stat d'origine du bijou (`config/jewelry/items_v8.json` du serveur). Émeraude = Chance −1…+2.
- Sac = moitié de `all_armor/*_armor_1` sans durabilité. Élytre des âmes = copie des 12 fichiers `elytra/` du jar.
- Matériau de reforgeage (`reforge_items/tieredtrinkets_*.json`) : pierre du bijou, lingot (anneaux fer/cuivre/or),
  diamant (diamant et bijoux uniques), émeraude, cuir (sacs, ids listés : `items` n'accepte pas de tag),
  membrane de phantom (Élytre des âmes). Sans fichier, TieredZ demande du silex (`tiered:reforge_base_item`).

## TieredZ 1.3.7 (relevé au javap, 2026-09-28)
- Palier = composant `Tiered.TIER` (`TierComponent(tier, durable, operation)`) ; bonus calculés au vol.
- Reforgeage : chaque palier +1 de poids → Unique (poids 0) possible ; Unique non reforgeable
  (`uniqueReforge: false`). Chance du joueur : `luckReforgeModifier` (serveur : 0.05).
- `AttributeTemplate` : attribut par id (`ResourceLocation.parse`), emplacements `EquipmentSlot`.

## Tests — TDD obligatoire
- `./gradlew build runGameTest` (CI idem). Rouge d'abord pour tout nouveau comportement.
- Joueur factice retiré dès sa création → Trinkets ne le tick pas : les tests appellent `TrinketModifiers.get`.
- Emplacements collier/anneaux activés pour le joueur par `src/gametest/resources/data/trinkets/entities/`
  (sur le serveur : Spell Engine, Critters and Companions…, absents des tests).
- Traveler's Backpack : version Modrinth par id (`npKsvQvE`), le numéro est partagé avec NeoForge.
- Jars embarqués non chargés en dev → Tiny Config, Jankson, owo-lib, Custom Portal API ajoutés à part.

## Release
Tag `vX.Y.Z` = `version` de `gradle.properties` → workflow `release` → jar sur la release GitHub.
