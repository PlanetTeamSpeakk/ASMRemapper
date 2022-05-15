package com.ptsmods.asmremapper.mappings;

/**
 * Enum indicating the type of a mapping.
 */
public enum MappingType {
	/**
	 * Class mapping
	 */
	CLASS,
	/**
	 * Method mapping
	 */
	METHOD,
	/**
	 * Field mapping
	 */
	FIELD,
	/**
	 * Parameter mapping (only used when parsing as they're not required for remapping dumps)
	 */
	PARAM;

	/**
	 * @param ch The character
	 * @return Gets the mapping type associated with a character (e.g. {@code CLASS} for c, {@code METHOD} for m, etc.)
	 */
	public static MappingType fromChar(char ch) {
		return switch (ch) {
			case 'c' -> CLASS;
			case 'm' -> METHOD;
			case 'f' -> FIELD;
			case 'p' -> PARAM;
			default -> throw new IllegalStateException("Unexpected value: " + ch);
		};
	}
}
