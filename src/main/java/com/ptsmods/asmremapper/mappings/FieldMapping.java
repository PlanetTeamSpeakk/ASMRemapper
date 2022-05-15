package com.ptsmods.asmremapper.mappings;

public record FieldMapping(ClassMapping owner, String descriptor, String officialDescriptor, String official, String intermediary, String named) implements Mapping {

	@Override
	public MappingType type() {
		return MappingType.FIELD;
	}
}
