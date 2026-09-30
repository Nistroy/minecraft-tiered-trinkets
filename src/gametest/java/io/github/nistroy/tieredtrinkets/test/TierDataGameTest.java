package io.github.nistroy.tieredtrinkets.test;

import draylar.tiered.Tiered;
import draylar.tiered.api.ItemVerifier;
import draylar.tiered.api.ModifierUtils;
import draylar.tiered.api.PotentialAttribute;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/** Les fichiers de paliers du mod visent des objets qui existent, et chacun peut passer au reforgeage. */
public class TierDataGameTest implements FabricGameTest {
	private static final TagKey<Item> NECKLACES = trinketTag("chest/necklace");
	private static final TagKey<Item> RINGS = trinketTag("hand/ring");
	private static final TagKey<Item> BACKPACKS = trinketTag("chest/back");

	@GameTest(template = EMPTY_STRUCTURE)
	public void everyJewelIsReforgeable(GameTestHelper helper) {
		List<Item> jewels = Stream.concat(tagged(NECKLACES), tagged(RINGS))
				.filter(item -> BuiltInRegistries.ITEM.getKey(item).getNamespace().equals("jewelry"))
				.distinct()
				.toList();
		helper.assertTrue(jewels.size() >= 50, "bijoux Jewelry attendus dans les tags collier/anneau : " + jewels.size());
		assertReforgeable(helper, jewels, "_trinket_");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void everyBackpackIsReforgeable(GameTestHelper helper) {
		List<Item> backpacks = tagged(BACKPACKS).toList();
		helper.assertTrue(backpacks.size() >= 40, "sacs à dos attendus dans le tag dos : " + backpacks.size());
		assertReforgeable(helper, backpacks, "_trinket_backpack");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void soulElytraIsReforgeable(GameTestHelper helper) {
		assertReforgeable(helper, List.of(TieredTrinketsGameTest.item("deeperdarker:soul_elytra")), "_soul_elytra_");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void shieldOfRepulsionIsReforgeable(GameTestHelper helper) {
		assertReforgeable(helper, List.of(TieredTrinketsGameTest.item("aether:shield_of_repulsion")), "_trinket_shield");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void regenerationStoneIsReforgeable(GameTestHelper helper) {
		assertReforgeable(helper, List.of(TieredTrinketsGameTest.item("aether:regeneration_stone")), "_trinket_health");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aetherAccessoriesReforgeWithZanite(GameTestHelper helper) {
		Item zanite = TieredTrinketsGameTest.item("aether:zanite_gemstone");
		for (String id : List.of("aether:shield_of_repulsion", "aether:regeneration_stone")) {
			List<Item> base = Tiered.REFORGE_DATA_LOADER.getReforgeBaseItems(TieredTrinketsGameTest.item(id));
			helper.assertTrue(base.equals(List.of(zanite)), "matériau de " + id + " : " + base);
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void emeraldNecklaceReforgesWithAnEmerald(GameTestHelper helper) {
		List<Item> base = Tiered.REFORGE_DATA_LOADER.getReforgeBaseItems(
				TieredTrinketsGameTest.item("jewelry:emerald_necklace"));
		helper.assertTrue(base.equals(List.of(Items.EMERALD)), "matériau du collier d'émeraude : " + base);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void everyTierTargetsExistingItems(GameTestHelper helper) {
		List<String> missing = new ArrayList<>();
		for (PotentialAttribute tier : Tiered.ATTRIBUTE_DATA_LOADER.getItemAttributes().values()) {
			if (!isModTier(tier.getID())) {
				continue;
			}
			for (ItemVerifier verifier : tier.getVerifiers()) {
				if (verifier.getId() != null && !BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(verifier.getId()))) {
					missing.add(tier.getID() + " → " + verifier.getId());
				}
			}
		}
		helper.assertTrue(missing.isEmpty(), "objets inexistants : " + missing);
		helper.succeed();
	}

	private static void assertReforgeable(GameTestHelper helper, List<Item> items, String tierMarker) {
		List<String> problems = new ArrayList<>();
		for (Item item : items) {
			String id = BuiltInRegistries.ITEM.getKey(item).toString();
			ResourceLocation tier = ModifierUtils.getRandomAttributeIDFor(null, item, true);
			if (tier == null || !tier.getPath().contains(tierMarker)) {
				problems.add(id + " : palier " + tier);
			}
			if (Tiered.REFORGE_DATA_LOADER.getReforgeBaseItems(item).isEmpty()) {
				problems.add(id + " : pas de matériau de reforgeage");
			}
		}
		helper.assertTrue(problems.isEmpty(), "non reforgeables : " + problems);
	}

	private static boolean isModTier(String id) {
		return id.contains("_trinket_") || id.contains("_soul_elytra_");
	}

	private static Stream<Item> tagged(TagKey<Item> tag) {
		return BuiltInRegistries.ITEM.getTag(tag).stream().flatMap(set -> set.stream().map(Holder::value));
	}

	private static TagKey<Item> trinketTag(String slot) {
		return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("trinkets", slot));
	}
}
