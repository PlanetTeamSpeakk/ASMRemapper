package com.ptsmods.asmremapper.mappings;

public record MethodMapping(ClassMapping owner, String signature, String officialSignature, String official, String intermediary, String named) implements Mapping {

	@Override
	public MappingType type() {
		return MappingType.METHOD;
	}
}
