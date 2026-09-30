package io.github.nistroy.tieredtrinkets;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public final class TieredTrinkets implements ModInitializer {
	@Override
	public void onInitialize() {
		// Trinkets ne charge TrinketModifiers qu'au premier objet équipé par un joueur. La charger ici applique
		// le mixin au démarrage : un crochet cassé plante le lancement du serveur, pas la partie en cours.
		try {
			Class.forName("dev.emi.trinkets.TrinketModifiers");
		} catch (ClassNotFoundException e) {
			throw new IllegalStateException("Trinkets sans TrinketModifiers : version non prise en charge", e);
		}
		// Accessories n'est là qu'avec Aether.
		if (FabricLoader.getInstance().isModLoaded("accessories")) {
			AccessoriesTiers.register();
		}
	}
}
