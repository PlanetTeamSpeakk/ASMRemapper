package com.ptsmods.asmremapper.mappings;

public record ClassMapping(String official, String intermediary, String named) implements Mapping {

	@Override
	public MappingType type() {
		return MappingType.CLASS;
	}

	public boolean isObfuscated() {
		return !(official().equals(intermediary()) && intermediary().equals(named()));
	}
}
