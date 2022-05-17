package com.ptsmods.asmremapper.mappings;

import com.ptsmods.asmremapper.util.Pair;

import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Object to hold class, field and method mappings
 */
public class Mappings {
	private final Type type;
	private final Map<String, ClassMapping> classMappings;
	private final Map<String, MethodMapping> methodMappings;
	private final Map<String, FieldMapping> fieldMappings;

	/**
	 * Constructs a new Mappings object.
	 * @param type The {@link Type} of these Mappings
	 * @param classMappings The parsed class mappings
	 * @param methodMappings The parsed method mappings
	 * @param fieldMappings The parsed field mappings
	 */
	public Mappings(Type type, Map<String, ClassMapping> classMappings, Map<Pair<ClassMapping, String>, MethodMapping> methodMappings, Map<Pair<ClassMapping, String>, FieldMapping> fieldMappings) {
		this.type = type;
		this.classMappings = Collections.unmodifiableMap(classMappings);
		this.methodMappings = Collections.unmodifiableMap(methodMappings.entrySet().stream()
				.collect(Collectors.toMap(entry -> type.formatKey(entry.getKey().left()) + ';' + entry.getKey().right(), Map.Entry::getValue)));
		this.fieldMappings = Collections.unmodifiableMap(fieldMappings.entrySet().stream()
				.collect(Collectors.toMap(entry -> type.formatKey(entry.getKey().left()) + ';' + entry.getKey().right(), Map.Entry::getValue)));
	}

	/**
	 * @param name The name of the class
	 * @return The {@link ClassMapping} requested
	 */
	public ClassMapping getClassMapping(String name) {
		return classMappings.get(name);
	}

	/**
	 * @param owner The owner of the method
	 * @param name The name of the method
	 * @param signature The signature of the method
	 * @return The {@link MethodMapping} requested
	 */
	public MethodMapping getMethodMapping(ClassMapping owner, String name, String signature) {
		return methodMappings.get(type.formatKey(owner) + ';' + name + signature);
	}

	/**
	 * @param owner The owner of the method
	 * @param name The name of the method
	 * @param signature The signature of the method
	 * @return The {@link MethodMapping} requested
	 */
	public MethodMapping getMethodMapping(String owner, String name, String signature) {
		return getMethodMapping(getClassMapping(owner), name, signature);
	}

	/**
	 * @param owner The owner of the field
	 * @param name The name of the field
	 * @return The {@link FieldMapping} requested
	 */
	public FieldMapping getFieldMapping(ClassMapping owner, String name) {
		return fieldMappings.get(type.formatKey(owner) + ';' + name);
	}

	/**
	 * @param owner The owner of the field
	 * @param name The name of the field
	 * @return The {@link FieldMapping} requested
	 */
	public FieldMapping getFieldMapping(String owner, String name) {
		return getFieldMapping(getClassMapping(owner), name);
	}

	/**
	 * @param owner The owner of the method
	 * @param name The name of the method
	 * @param signature The signature of the method
	 * @return Whether the given owner has a method of the given name and signature.
	 */
	public boolean hasMethod(ClassMapping owner, String name, String signature) {
		return methodMappings.containsKey(type.formatKey(owner) + ';' + name + signature);
	}

	/**
	 * @param owner The owner of the method
	 * @param name The name of the method
	 * @param signature The signature of the method
	 * @return Whether the given owner has a method of the given name and signature.
	 */
	public boolean hasMethod(String owner, String name, String signature) {
		return hasMethod(getClassMapping(owner), name, signature);
	}

	/**
	 * Enum indicating what type a Mappings object is.
	 */
	public enum Type {
		/**
		 * Yarn mappings, keys are named.
		 */
		YARN(ClassMapping::named),
		/**
		 * Moj mappings, keys are official
		 */
		MOJ(ClassMapping::official);

		private final Function<ClassMapping, String> keyFormatter;

		Type(Function<ClassMapping, String> keyFormatter) {
			this.keyFormatter = keyFormatter;
		}

		/**
		 * Formats the key used to store cache.
		 * @param classMapping The mapping to format.
		 * @return The key used to store cache.
		 */
		public String formatKey(ClassMapping classMapping) {
			return keyFormatter.apply(classMapping);
		}
	}
}
