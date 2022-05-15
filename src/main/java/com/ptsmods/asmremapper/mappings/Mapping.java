package com.ptsmods.asmremapper.mappings;

public interface Mapping {
	MappingType type();

	String intermediary();

	String named();
}
