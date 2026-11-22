package com.a86151632hub.judgmentblade;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ToolMaterial;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JudgmentBladeMod implements ModInitializer {
	public static final String MOD_ID = "judgmentblade";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final RegistryKey<Item> JUDGMENT_BLADE_KEY = RegistryKey.of(
			RegistryKeys.ITEM,
			Identifier.of(MOD_ID, "judgment_blade")
	);

	public static final Item JUDGMENT_BLADE = Registry.register(
			Registries.ITEM,
			JUDGMENT_BLADE_KEY,
			new JudgmentBladeItem(
					ToolMaterial.NETHERITE,
					12,
					-2.0f,
					new Item.Settings()
							.registryKey(JUDGMENT_BLADE_KEY)
							.rarity(Rarity.EPIC)
							.fireproof()
			)
	);

	@Override
	public void onInitialize() {
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(entries -> {
			entries.add(JUDGMENT_BLADE);
		});
		LOGGER.info("Judgment Blade Mod initialized!");
	}
}
