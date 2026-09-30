package io.github.nistroy.tieredtrinkets;

import io.wispforest.accessories.api.events.AdjustAttributeModifierCallback;
import io.wispforest.accessories.api.slot.SlotReference;
import net.minecraft.resources.ResourceLocation;

import java.util.Locale;

/**
 * Accessories (embarqué par Aether) passe par {@code AdjustAttributeModifierCallback} pour les bonus d'un objet,
 * à l'équipement comme au déséquipement et dans l'infobulle : y ajouter le palier suffit.
 */
final class AccessoriesTiers {
	private AccessoriesTiers() {
	}

	static void register() {
		AdjustAttributeModifierCallback.EVENT.register((stack, slot, builder) ->
				WornTierModifiers.forEach(stack, slotId(slot), builder::addExclusive));
	}

	// Nom d'emplacement Accessories = « aether:shield » ; « : » est interdit dans un chemin d'id.
	private static ResourceLocation slotId(SlotReference slot) {
		String name = slot.slotName().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9/._-]", "/");
		return ResourceLocation.fromNamespaceAndPath("accessories", name + "/" + slot.slot());
	}
}
