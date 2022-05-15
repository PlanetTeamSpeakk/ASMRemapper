package com.ptsmods.asmremapper.util;

import java.util.List;

public record Descriptor(Class<?> returnType, List<Class<?>> parameterTypes) {
}
