package io.github.nistroy.tieredtrinkets.mixin;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.emi.trinkets.TrinketModifiers;
import dev.emi.trinkets.api.SlotReference;
import io.github.nistroy.tieredtrinkets.TrinketTierModifiers;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Trinkets passe par {@code TrinketModifiers.get} pour ajouter les bonus d'un objet équipé et pour les retirer
 * au déséquipement : y ajouter le palier suffit. Les deux surcharges de {@code get} sont visées.
 */
@Mixin(value = TrinketModifiers.class, remap = false)
abstract class TrinketModifiersMixin {
	// `get*` : un nom seul ne vise que la première surcharge, et les descripteurs changent entre dev et serveur.
	@ModifyReturnValue(method = "get*", at = @At("RETURN"), require = 2)
	private static Multimap<Holder<Attribute>, AttributeModifier> tieredtrinkets$addTier(
			Multimap<Holder<Attribute>, AttributeModifier> modifiers,
			@Local(argsOnly = true) ItemStack stack, @Local(argsOnly = true) SlotReference slot) {
		// La map rendue par Trinket#getModifiers peut être immuable selon l'objet.
		Multimap<Holder<Attribute>, AttributeModifier> withTier = LinkedHashMultimap.create(modifiers);
		TrinketTierModifiers.addTo(withTier, stack, slot);
		return withTier;
	}
}
