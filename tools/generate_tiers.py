#!/usr/bin/env python3
"""Génère les paliers TieredZ des bijoux, sacs à dos et Élytre des âmes, et leurs noms pour le pack de ressources.

Usage : tools/generate_tiers.py <tiered-1.3.7.jar>
Écrit src/main/resources/data/tiered/ et lang/en_us.json (à copier dans le pack de ressources du serveur :
les noms de palier sont des traductions côté client, le mod ne tourne que sur le serveur).
"""
import json
import shutil
import sys
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DATA = ROOT / "src/main/resources/data/tiered"
TIERS = DATA / "item_attributes/tieredtrinkets"
REFORGE = DATA / "reforge_items"
LANG = ROOT / "lang/en_us.json"

# Poids et couleurs de TieredZ ; l'Unique ne sort qu'au reforgeage (poids 0, +1 au reforgeage).
TIER_ORDER = ["common", "uncommon", "rare", "epic", "legendary", "unique"]
WEIGHT = {"common": 50, "uncommon": 35, "rare": 15, "epic": 8, "legendary": 3, "unique": 0}
COLOR = {"common": "gray", "uncommon": "dark_green", "rare": "blue", "epic": "dark_purple",
         "legendary": "gold", "unique": "light_purple"}
LABEL = {"common": "Common", "uncommon": "Uncommon", "rare": "Rare", "epic": "Epic",
         "legendary": "Legendary", "unique": "Unique"}

# Emplacement jamais porté par un joueur : TieredZ n'applique pas ces bonus en main, le mod les applique
# dans les emplacements Trinkets.
WORN = ["BODY"]

# Bijoux : moitié des paliers d'arme. Bonus principal (le thème du bijou) puis bonus secondaire dès Épique.
PERCENT_MAIN = {"common": -0.15, "uncommon": -0.05, "rare": 0.05, "epic": 0.05, "legendary": 0.10, "unique": 0.15}
CRIT_SECOND = {"epic": 0.05, "legendary": 0.075, "unique": 0.10}
FLAT_ARMOR = {"common": -1, "uncommon": -0.5, "rare": 0.5, "epic": 1, "legendary": 1.5, "unique": 2}
FLAT_HEALTH = {"common": -2, "uncommon": -1, "rare": 1, "epic": 2, "legendary": 3, "unique": 4}
FLAT_LUCK = {"common": -1, "uncommon": -0.5, "rare": 0.5, "epic": 1, "legendary": 1.5, "unique": 2}
TOUGHNESS_SECOND = {"epic": 0.5, "legendary": 0.5, "unique": 1}
RANGED_HASTE_SECOND = {"epic": 0.02, "legendary": 0.03, "unique": 0.04}

SPELL_CRIT = ("spell_power:critical_chance", "ADD_MULTIPLIED_BASE", CRIT_SECOND)
MELEE_CRIT = ("tiered:generic.crit_chance", "ADD_MULTIPLIED_TOTAL", CRIT_SECOND)
TOUGHNESS = ("generic.armor_toughness", "ADD_VALUE", TOUGHNESS_SECOND)


def percent(*attributes):
    return [(attribute, "ADD_MULTIPLIED_TOTAL", PERCENT_MAIN) for attribute in attributes]


# thème → (bonus principaux, bonus secondaire ou None)
THEMES = {
    "luck": ([("generic.luck", "ADD_VALUE", FLAT_LUCK)], None),
    "armor": ([("generic.armor", "ADD_VALUE", FLAT_ARMOR)], TOUGHNESS),
    "health": ([("generic.max_health", "ADD_VALUE", FLAT_HEALTH)], TOUGHNESS),
    "melee": (percent("generic.attack_damage"), MELEE_CRIT),
    "attack_speed": (percent("generic.attack_speed"), MELEE_CRIT),
    "ranged": (percent("ranged_weapon:damage"),
               ("ranged_weapon:haste", "ADD_MULTIPLIED_BASE", RANGED_HASTE_SECOND)),
    "fire_arcane": (percent("spell_power:fire", "spell_power:arcane"), SPELL_CRIT),
    "fire": (percent("spell_power:fire"), SPELL_CRIT),
    "arcane": (percent("spell_power:arcane"), SPELL_CRIT),
    "frost_soul": (percent("spell_power:frost", "spell_power:soul"), SPELL_CRIT),
    "frost": (percent("spell_power:frost"), SPELL_CRIT),
    "healing_lightning": (percent("spell_power:healing", "spell_power:lightning"), SPELL_CRIT),
    "healing": (percent("spell_power:healing"), SPELL_CRIT),
    "spell": (percent("spell_power:generic"), SPELL_CRIT),
}


def both(*gems):
    """Anneau et collier, normaux et netherite, de chaque pierre."""
    return [f"jewelry:{prefix}{gem}_{kind}" for gem in gems for prefix in ("", "netherite_")
            for kind in ("ring", "necklace")]


def unique(*names):
    return [f"jewelry:unique_{name}_{kind}" for name in names for kind in ("ring", "necklace")]


# thème → bijoux (config Jewelry `items_v8.json` du serveur, lue le 2026-09-28 : chaque bijou garde son thème)
JEWELS = {
    "luck": ["jewelry:emerald_necklace"],
    "armor": ["jewelry:iron_ring", "jewelry:copper_ring", "jewelry:gold_ring"],
    "health": both("sapphire") + unique("tank"),
    "melee": both("ruby") + unique("attack", "crit"),
    "attack_speed": ["jewelry:diamond_ring", "jewelry:diamond_necklace"] + unique("dex"),
    "ranged": both("jade") + unique("archer"),
    "fire_arcane": both("topaz"),
    "fire": unique("fire"),
    "arcane": unique("arcane"),
    "frost_soul": both("tanzanite"),
    "frost": unique("frost"),
    "healing_lightning": both("citrine"),
    "healing": unique("healing"),
    "spell": unique("spell"),
}

# Sac à dos : moitié du palier d'armure 1 de TieredZ (sans durabilité : les sacs ne s'usent pas).
BACKPACK = {
    "common": [("generic.max_health", "ADD_VALUE", -1)],
    "uncommon": [("generic.armor", "ADD_VALUE", 0.5)],
    "rare": [("generic.armor", "ADD_VALUE", 0.5), ("generic.max_health", "ADD_VALUE", -0.5)],
    "epic": [("generic.armor_toughness", "ADD_VALUE", 0.5), ("generic.max_health", "ADD_VALUE", 1)],
    "legendary": [("generic.armor", "ADD_VALUE", 1), ("generic.armor_toughness", "ADD_VALUE", 0.5),
                  ("generic.max_health", "ADD_VALUE", 2)],
    "unique": [("generic.armor", "ADD_VALUE", 2), ("generic.armor_toughness", "ADD_VALUE", 1),
               ("generic.max_health", "ADD_VALUE", 3)],
}
BACKPACK_TAG = "trinkets:chest/back"

# Matériau posé à côté de l'objet au reforgeage (sinon TieredZ demande du silex).
REFORGE_BASE = {
    "minecraft:emerald": ["jewelry:emerald_necklace"],
    "minecraft:iron_ingot": ["jewelry:iron_ring"],
    "minecraft:copper_ingot": ["jewelry:copper_ring"],
    "minecraft:gold_ingot": ["jewelry:gold_ring"],
    "minecraft:diamond": ["jewelry:diamond_ring", "jewelry:diamond_necklace"]
                         + unique("tank", "attack", "crit", "dex", "archer", "fire", "arcane", "frost",
                                  "healing", "spell"),
    "jewelry:sapphire": both("sapphire"),
    "jewelry:ruby": both("ruby"),
    "jewelry:jade": both("jade"),
    "jewelry:topaz": both("topaz"),
    "jewelry:tanzanite": both("tanzanite"),
    "jewelry:citrine": both("citrine"),
    "minecraft:phantom_membrane": ["deeperdarker:soul_elytra"],
}


def attribute(tier_id, attribute_type, operation, amount, slots):
    return {"type": attribute_type,
            "modifier": {"name": tier_id, "operation": operation, "amount": amount},
            "optional_equipment_slots": slots}


def tier_file(tier_id, tier, verifiers, attributes):
    return {"id": tier_id, "verifiers": verifiers, "weight": WEIGHT[tier], "style": {"color": COLOR[tier]},
            "attributes": attributes}


def write(path, content):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(content, indent=4, ensure_ascii=False) + "\n")


def jewel_tiers(labels):
    for theme, items in JEWELS.items():
        mains, second = THEMES[theme]
        verifiers = [{"id": item} for item in items]
        for tier in TIER_ORDER:
            tier_id = f"tiered:{tier}_trinket_{theme}"
            attributes = [attribute(tier_id, kind, op, amounts[tier], WORN) for kind, op, amounts in mains]
            if second and tier in second[2]:
                attributes.append(attribute(tier_id, second[0], second[1], second[2][tier], WORN))
            write(TIERS / f"{tier}_trinket_{theme}.json", tier_file(tier_id, tier, verifiers, attributes))
            labels[f"{tier_id}.label"] = LABEL[tier]


def backpack_tiers(labels):
    for tier in TIER_ORDER:
        tier_id = f"tiered:{tier}_trinket_backpack"
        attributes = [attribute(tier_id, kind, op, amount, WORN) for kind, op, amount in BACKPACK[tier]]
        write(TIERS / f"{tier}_trinket_backpack.json",
              tier_file(tier_id, tier, [{"tag": BACKPACK_TAG}], attributes))
        labels[f"{tier_id}.label"] = LABEL[tier]


def soul_elytra_tiers(tiered_jar, labels):
    """Copie des paliers d'élytre de TieredZ (portés au torse) pour l'Élytre des âmes de Deeper and Darker."""
    prefix = "data/tiered/item_attributes/elytra/"
    with zipfile.ZipFile(tiered_jar) as jar:
        for name in sorted(n for n in jar.namelist() if n.startswith(prefix) and n.endswith(".json")):
            source = json.loads(jar.read(name))
            stem = Path(name).stem.replace("_elytra_", "_soul_elytra_")  # ex. unique_soul_elytra_1
            tier_id = f"tiered:{stem}"
            source["id"] = tier_id
            source["verifiers"] = [{"id": "deeperdarker:soul_elytra"}]
            for entry in source["attributes"]:
                entry["modifier"]["name"] = tier_id
            write(TIERS / f"{stem}.json", source)
            labels[f"{tier_id}.label"] = LABEL[stem.split("_")[0]]


def reforge_items():
    for base, items in REFORGE_BASE.items():
        write(REFORGE / f"tieredtrinkets_{base.split(':')[1]}.json", {"items": items, "base": [base]})
    # `items` n'accepte que des ids : les 44 sacs du tag `trinkets:chest/back` de Traveler's Backpack 10.1.39.
    write(REFORGE / "tieredtrinkets_leather.json", {"items": BACKPACKS, "base": ["minecraft:leather"]})


BACKPACKS = [f"travelersbackpack:{name}" for name in (
    "standard", "netherite", "diamond", "gold", "emerald", "iron", "lapis", "redstone", "coal", "quartz",
    "bookshelf", "end", "nether", "sandstone", "snow", "sponge", "cake", "cactus", "hay", "melon", "pumpkin",
    "creeper", "dragon", "enderman", "blaze", "ghast", "magma_cube", "skeleton", "spider", "wither", "warden",
    "bat", "bee", "wolf", "fox", "ocelot", "horse", "cow", "pig", "sheep", "chicken", "squid", "villager",
    "iron_golem")]


def main():
    if len(sys.argv) != 2:
        sys.exit(__doc__)
    shutil.rmtree(TIERS, ignore_errors=True)
    for old in REFORGE.glob("tieredtrinkets_*.json"):
        old.unlink()
    labels = {}
    jewel_tiers(labels)
    backpack_tiers(labels)
    soul_elytra_tiers(sys.argv[1], labels)
    reforge_items()
    write(LANG, dict(sorted(labels.items())))
    print(f"{len(list(TIERS.glob('*.json')))} paliers, {len(labels)} noms")


if __name__ == "__main__":
    main()
