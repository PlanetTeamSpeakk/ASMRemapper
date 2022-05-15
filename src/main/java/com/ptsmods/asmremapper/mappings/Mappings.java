package com.ptsmods.asmremapper.mappings;

import com.ptsmods.asmremapper.util.Pair;

import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Mappings {
	private final Type type;
	private final Map<String, ClassMapping> classMappings;
	private final Map<String, MethodMapping> methodMappings;
	private final Map<String, FieldMapping> fieldMappings;

	public Mappings(Type type, Map<String, ClassMapping> classMappings, Map<Pair<ClassMapping, String>, MethodMapping> methodMappings, Map<Pair<ClassMapping, String>, FieldMapping> fieldMappings) {
		this.type = type;
		this.classMappings = Collections.unmodifiableMap(classMappings);
		this.methodMappings = Collections.unmodifiableMap(methodMappings.entrySet().stream()
				.collect(Collectors.toMap(entry -> type.formatKey(entry.getKey().left()) + ';' + entry.getKey().right(), Map.Entry::getValue)));
		this.fieldMappings = Collections.unmodifiableMap(fieldMappings.entrySet().stream()
				.collect(Collectors.toMap(entry -> type.formatKey(entry.getKey().left()) + ';' + entry.getKey().right(), Map.Entry::getValue)));
	}

	public ClassMapping getClassMapping(String name) {
		return classMappings.get(name);
	}

	public MethodMapping getMethodMapping(ClassMapping owner, String name, String signature) {
		return methodMappings.get(type.formatKey(owner) + ';' + name + signature);
	}

	public MethodMapping getMethodMapping(String owner, String name, String signature) {
		return getMethodMapping(getClassMapping(owner), name, signature);
	}

	public FieldMapping getFieldMapping(ClassMapping owner, String name) {
		return fieldMappings.get(type.formatKey(owner) + ';' + name);
	}

	public FieldMapping getFieldMapping(String owner, String name) {
		return getFieldMapping(getClassMapping(owner), name);
	}

	public boolean hasMethod(ClassMapping owner, String name, String signature) {
		return methodMappings.containsKey(type.formatKey(owner) + ';' + name + signature);
	}

	public enum Type {
		YARN(ClassMapping::named),
		MOJ(ClassMapping::official);

		private final Function<ClassMapping, String> keyFormatter;

		Type(Function<ClassMapping, String> keyFormatter) {
			this.keyFormatter = keyFormatter;
		}

		public String formatKey(ClassMapping classMapping) {
			return keyFormatter.apply(classMapping);
		}
	}
}
