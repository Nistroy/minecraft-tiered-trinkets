package io.github.nistroy.tieredtrinkets.test;

import com.google.common.collect.Multimap;
import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.slot.SlotType;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Accessories (embarqué par Aether) calcule les bonus d'un emplacement avec
 * {@link AccessoriesAPI#getAttributeModifiers}, à l'équipement comme au déséquipement : les tests l'appellent.
 */
public class AccessoriesTierGameTest implements FabricGameTest {
	private static final String MOD = "tieredtrinkets";
	private static final String SHIELD = "aether:shield_of_repulsion";

	@GameTest(template = EMPTY_STRUCTURE)
	public void tieredShieldGivesItsTierBonus(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		ItemStack shield = TieredTrinketsGameTest.tiered(TieredTrinketsGameTest.item(SHIELD), "tiered:unique_trinket_shield");

		Collection<AttributeModifier> armor = tierModifiers(bonuses(player, shield), Attributes.ARMOR);

		helper.assertTrue(armor.size() == 1 && armor.iterator().next().amount() == 2,
				"Bouclier de répulsion Unique : +2 armure attendu, obtenu " + armor);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void untieredShieldGetsNoTierBonus(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();

		Multimap<Holder<Attribute>, AttributeModifier> bonuses =
				bonuses(player, new ItemStack(TieredTrinketsGameTest.item(SHIELD)));

		helper.assertTrue(bonuses.values().stream().noneMatch(AccessoriesTierGameTest::isTierModifier),
				"sans palier, aucun bonus de palier : " + bonuses);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void twoRegenerationStonesKeepTheirOwnBonus(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		ItemStack stone = TieredTrinketsGameTest.tiered(TieredTrinketsGameTest.item("aether:regeneration_stone"),
				"tiered:unique_trinket_health");

		Set<ResourceLocation> first = ids(tierModifiers(bonuses(player, stone, 0), Attributes.MAX_HEALTH));
		Set<ResourceLocation> second = ids(tierModifiers(bonuses(player, stone, 1), Attributes.MAX_HEALTH));

		helper.assertTrue(first.size() == 1 && second.size() == 1 && !first.equals(second),
				"deux pierres de régénération au même palier : un bonus chacune, ids distincts ("
						+ first + " / " + second + ")");
		helper.succeed();
	}

	private static Multimap<Holder<Attribute>, AttributeModifier> bonuses(ServerPlayer player, ItemStack stack) {
		return bonuses(player, stack, 0);
	}

	private static Multimap<Holder<Attribute>, AttributeModifier> bonuses(ServerPlayer player, ItemStack stack,
			int index) {
		SlotType slot = AccessoriesAPI.getStackSlotTypes(player, stack).stream().findFirst()
				.orElseThrow(() -> new IllegalStateException("aucun emplacement Accessories pour " + stack));
		return AccessoriesAPI.getAttributeModifiers(stack, player, slot.name(), index).getAttributeModifiers(false);
	}

	private static Set<ResourceLocation> ids(Collection<AttributeModifier> modifiers) {
		return modifiers.stream().map(AttributeModifier::id).collect(Collectors.toSet());
	}

	private static Collection<AttributeModifier> tierModifiers(Multimap<Holder<Attribute>, AttributeModifier> bonuses,
			Holder<Attribute> attribute) {
		return bonuses.get(attribute).stream().filter(AccessoriesTierGameTest::isTierModifier).toList();
	}

	private static boolean isTierModifier(AttributeModifier modifier) {
		return modifier.id().getNamespace().equals(MOD);
	}
}
