package com.ptsmods.asmremapper.mappings;

public enum MappingType {
	CLASS, METHOD, FIELD, PARAM;

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
