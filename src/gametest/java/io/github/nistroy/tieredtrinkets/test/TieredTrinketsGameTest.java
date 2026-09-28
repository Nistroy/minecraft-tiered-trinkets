package io.github.nistroy.tieredtrinkets.test;

import com.google.common.collect.Multimap;
import dev.emi.trinkets.TrinketModifiers;
import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.TrinketInventory;
import dev.emi.trinkets.api.TrinketsApi;
import draylar.tiered.Tiered;
import draylar.tiered.api.TierComponent;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Trinkets calcule les bonus d'un emplacement avec {@link TrinketModifiers#get}, à l'équipement comme au
 * déséquipement. Le joueur factice n'est pas « tické » par Trinkets (retiré dès sa création) : les tests
 * appellent donc directement cette méthode.
 */
public class TieredTrinketsGameTest implements FabricGameTest {
	private static final String MOD = "tieredtrinkets";

	@GameTest(template = EMPTY_STRUCTURE)
	public void tieredNecklaceGivesItsTierBonus(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		ItemStack necklace = tiered(item("jewelry:emerald_necklace"), "tiered:unique_trinket_luck");

		Collection<AttributeModifier> luck = tierModifiers(bonuses(player, necklace, "chest", "necklace", 0), Attributes.LUCK);

		helper.assertTrue(luck.size() == 1 && luck.iterator().next().amount() == 2,
				"collier d'émeraude Unique : +2 Chance attendu, obtenu " + luck);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void untieredTrinketGetsNoTierBonus(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		ItemStack necklace = new ItemStack(item("jewelry:emerald_necklace"));

		Multimap<Holder<Attribute>, AttributeModifier> bonuses = bonuses(player, necklace, "chest", "necklace", 0);

		helper.assertTrue(bonuses.values().stream().noneMatch(TieredTrinketsGameTest::isTierModifier),
				"sans palier, aucun bonus de palier : " + bonuses);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void twoRingsKeepTheirOwnBonus(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		String tier = "tiered:unique_trinket_armor";

		Set<ResourceLocation> hand = ids(tierModifiers(
				bonuses(player, tiered(item("jewelry:iron_ring"), tier), "hand", "ring", 0), Attributes.ARMOR));
		Set<ResourceLocation> offhand = ids(tierModifiers(
				bonuses(player, tiered(item("jewelry:iron_ring"), tier), "offhand", "ring", 0), Attributes.ARMOR));

		helper.assertTrue(hand.size() == 1 && offhand.size() == 1 && !hand.equals(offhand),
				"deux anneaux au même palier : un bonus chacun, ids distincts (" + hand + " / " + offhand + ")");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void capeElytraGetsItsChestTier(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		ItemStack elytra = tiered(Items.ELYTRA, "tiered:unique_elytra_1");

		Collection<AttributeModifier> health =
				tierModifiers(bonuses(player, elytra, "chest", "cape", 0), Attributes.MAX_HEALTH);

		helper.assertTrue(health.size() == 1 && health.iterator().next().amount() == 6,
				"élytre Unique dans l'emplacement cape : +6 PV (palier prévu pour le torse), obtenu " + health);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void handOnlyTierIsNotAppliedInATrinketSlot(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		ItemStack stick = tiered(Items.STICK, "tieredtrinkets_test:hand_only");

		Collection<AttributeModifier> luck =
				tierModifiers(bonuses(player, stick, "chest", "necklace", 0), Attributes.LUCK);

		helper.assertTrue(luck.isEmpty(), "un palier prévu pour la main ne compte pas dans un emplacement Trinkets : " + luck);
		helper.succeed();
	}

	static net.minecraft.world.item.Item item(String id) {
		return BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(id))
				.orElseThrow(() -> new IllegalStateException("objet absent des tests : " + id));
	}

	static ItemStack tiered(net.minecraft.world.item.Item item, String tier) {
		ItemStack stack = new ItemStack(item);
		// Comme `/tiered tier` : durabilité -1 = pas de bonus de durabilité.
		stack.set(Tiered.TIER, new TierComponent(tier, -1f, 0));
		return stack;
	}

	private static Multimap<Holder<Attribute>, AttributeModifier> bonuses(ServerPlayer player, ItemStack stack,
			String group, String slot, int index) {
		TrinketInventory inventory = TrinketsApi.getTrinketComponent(player).orElseThrow()
				.getInventory().get(group).get(slot);
		return TrinketModifiers.get(stack, new SlotReference(inventory, index), player);
	}

	private static Collection<AttributeModifier> tierModifiers(Multimap<Holder<Attribute>, AttributeModifier> bonuses,
			Holder<Attribute> attribute) {
		return bonuses.get(attribute).stream().filter(TieredTrinketsGameTest::isTierModifier).toList();
	}

	private static boolean isTierModifier(AttributeModifier modifier) {
		return modifier.id().getNamespace().equals(MOD);
	}

	private static Set<ResourceLocation> ids(Collection<AttributeModifier> modifiers) {
		return modifiers.stream().map(AttributeModifier::id).collect(Collectors.toSet());
	}
}
