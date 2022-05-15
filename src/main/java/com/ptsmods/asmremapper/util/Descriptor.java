package com.ptsmods.asmremapper.util;

import java.util.List;

/**
 * Simple class to hold a parsed method descriptor
 * @param returnType The returntype of a method
 * @param parameterTypes A list of classes resembling the parameter types of a method
 */
public record Descriptor(Class<?> returnType, List<Class<?>> parameterTypes) {}
