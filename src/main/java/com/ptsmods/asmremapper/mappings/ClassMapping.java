package com.ptsmods.asmremapper.mappings;

/**
 * @param official The official name of this class
 * @param intermediary The intermediary name of this class (if it's a Yarn mapping)
 * @param named The named name of this class
 */
public record ClassMapping(String official, String intermediary, String named) implements Mapping {

	@Override
	public MappingType type() {
		return MappingType.CLASS;
	}

	/**
	 * @return Whether this class is actually obfuscated. (False for e.g. net.minecraft.client.main.Main and net.minecraft.server.MinecraftServer)
	 */
	public boolean isObfuscated() {
		return !(official().equals(intermediary()) && intermediary().equals(named()));
	}
}
