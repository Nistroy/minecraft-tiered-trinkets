package io.github.nistroy.tieredtrinkets;

import draylar.tiered.Tiered;
import draylar.tiered.api.AttributeTemplate;
import draylar.tiered.api.ModifierUtils;
import draylar.tiered.api.PotentialAttribute;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

/** Bonus du palier TieredZ d'un objet porté dans un emplacement Trinkets ou Accessories. */
public final class WornTierModifiers {
	public static final String MOD_ID = "tieredtrinkets";

	/**
	 * TieredZ n'applique un palier qu'aux emplacements vanilla qu'il liste. Hors emplacement vanilla, on garde
	 * les bonus prévus pour le torse (élytre, portée au torse ou dans l'emplacement cape) et pour le corps : c'est
	 * l'emplacement des paliers de bijoux, de sacs et d'accessoires d'Aether, qu'aucun joueur ne porte, donc jamais
	 * appliqué en main.
	 */
	private static final Set<EquipmentSlot> WORN_AS = Set.of(EquipmentSlot.CHEST, EquipmentSlot.BODY);

	private WornTierModifiers() {
	}

	/** {@code slotId} identifie l'emplacement : il entre dans l'id des bonus. */
	public static void forEach(ItemStack stack, ResourceLocation slotId,
			BiConsumer<Holder<Attribute>, AttributeModifier> sink) {
		ResourceLocation tierId = ModifierUtils.getAttributeId(stack);
		if (tierId == null) {
			return;
		}
		PotentialAttribute tier = Tiered.ATTRIBUTE_DATA_LOADER.getItemAttributes().get(tierId);
		if (tier == null) {
			return;
		}
		List<AttributeTemplate> templates = tier.getAttributes();
		for (int i = 0; i < templates.size(); i++) {
			AttributeTemplate template = templates.get(i);
			if (!isWornOutsideVanillaSlots(template)) {
				continue;
			}
			int index = i;
			BuiltInRegistries.ATTRIBUTE.getHolder(ResourceLocation.parse(template.getAttributeTypeID()))
					.ifPresent(attribute -> sink.accept(attribute, forSlot(template, slotId, index)));
		}
	}

	private static boolean isWornOutsideVanillaSlots(AttributeTemplate template) {
		return lists(template.getRequiredEquipmentSlots()) || lists(template.getOptionalEquipmentSlots());
	}

	private static boolean lists(EquipmentSlot[] slots) {
		if (slots == null) {
			return false;
		}
		for (EquipmentSlot slot : slots) {
			if (WORN_AS.contains(slot)) {
				return true;
			}
		}
		return false;
	}

	// Un id par emplacement et par bonus : deux anneaux au même palier ne s'écrasent pas.
	private static AttributeModifier forSlot(AttributeTemplate template, ResourceLocation slotId, int index) {
		AttributeModifier modifier = template.getEntityAttributeModifier();
		ResourceLocation id = ResourceLocation.fromNamespaceAndPath(MOD_ID,
				slotId.getNamespace() + "/" + slotId.getPath() + "/" + index);
		return new AttributeModifier(id, modifier.amount(), modifier.operation());
	}
}
