package com.ptsmods.asmremapper.mappings;

/**
 * Mapping of a field
 * @param owner The mapping of the class that owns this field
 * @param descriptor The fully qualified type of this field
 * @param officialDescriptor The official name of the fully qualified type of this field
 * @param official The official name of this field
 * @param intermediary The intermediary name of this field (if this mapping is a Yarn mapping, otherwise null)
 * @param named The named name of this field
 */
public record FieldMapping(ClassMapping owner, String descriptor, String officialDescriptor, String official, String intermediary, String named) implements Mapping {

	@Override
	public MappingType type() {
		return MappingType.FIELD;
	}
}
