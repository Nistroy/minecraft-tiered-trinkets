# minecraft-tiered-trinkets

Petit mod **Fabric 1.21.1** pour le serveur entre copains. Avec
[TieredZ](https://modrinth.com/mod/tieredz), on reforge ses armes et armures à l'enclume pour leur donner un
palier (de Commun à Unique). Mais ces paliers ne comptaient pas pour les objets portés dans les emplacements
[Trinkets](https://modrinth.com/mod/trinkets) : collier, anneau, sac à dos, élytre.

Ce mod les fait compter, et ajoute des paliers pour :

- les **bijoux** (Jewelry) : chaque bijou renforce son thème (rubis = dégâts, topaze = feu et arcane,
  saphir = cœurs, émeraude = Chance…), à moitié de la force d'un palier d'arme ;
- les **sacs à dos** (Traveler's Backpack) : armure, ténacité et cœurs, à moitié d'un palier d'armure ;
- l'**Élytre des âmes** (Deeper and Darker) : mêmes paliers que l'élytre vanilla.

| Palier | Topaze (exemple) | Collier d'émeraude | Sac à dos |
| --- | --- | --- | --- |
| Commun | −15 % feu et arcane | −1 Chance | −½ cœur |
| Légendaire | +10 % feu et arcane, +7,5 % critique de sort | +1,5 Chance | +1 armure, +0,5 ténacité, +1 cœur |
| Unique | +15 % feu et arcane, +10 % critique de sort | +2 Chance | +2 armure, +1 ténacité, +1,5 cœur |

- Les bonus s'appliquent grâce au serveur. Installé aussi chez les joueurs (pack du serveur), le mod affiche
  les bonus du palier dans l'infobulle « Quand porté comme… » de Trinkets. Les noms des paliers sont fournis
  par le pack de ressources du serveur.
- Reforgeage : onglet « Reforger » de l'enclume, 1 éclat d'améthyste + le matériau du bijou (sa pierre, un
  lingot pour les anneaux simples, un diamant pour les bijoux uniques), du cuir pour un sac, une membrane de
  phantom pour l'Élytre des âmes.

Dépend de Trinkets `3.10.0` et de TieredZ `1.3.7`.

## Construire

```
tools/generate_tiers.py <chemin de tiered-1.3.7.jar>   # après un changement des paliers
./gradlew build runGameTest
```

Le jar est dans `build/libs/`. Licence GPL-3.0.
