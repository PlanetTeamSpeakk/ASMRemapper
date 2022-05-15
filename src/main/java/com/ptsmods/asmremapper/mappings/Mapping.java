package com.ptsmods.asmremapper.mappings;

/**
 * Interface containing values all mappings must have, regardless of type.
 */
public interface Mapping {
	/**
	 * @return The type of this mapping
	 */
	MappingType type();

	/**
	 * @return The official name of this mapping
	 */
	String official();

	/**
	 * @return The intermediary name of this mapping (if it's a Yarn mapping)
	 */
	String intermediary();

	/**
	 * @return The named name of this mapping
	 */
	String named();
}
