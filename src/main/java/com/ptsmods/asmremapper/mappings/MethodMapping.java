package com.ptsmods.asmremapper.mappings;

/**
 * Mapping of a method
 * @param owner The mapping of the class that owns this method
 * @param signature The signature of this method
 * @param officialSignature The signature of this method mapped to official names
 * @param official The official name of this method
 * @param intermediary The intermediary name of this method (if this mapping is a Yarn mapping, otherwise null)
 * @param named The named name of this method
 */
public record MethodMapping(ClassMapping owner, String signature, String officialSignature, String official, String intermediary, String named) implements Mapping {

	@Override
	public MappingType type() {
		return MappingType.METHOD;
	}
}
