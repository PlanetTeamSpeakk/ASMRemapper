package com.ptsmods.asmremapper.util;

import java.util.Objects;

public record Pair<L, R>(L left, R right) {
	public static <L, R> Pair<L, R> of(L left, R right) {
		return new Pair<>(left, right);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Pair<?, ?> pair)) return false;
		return Objects.equals(left, pair.left) && Objects.equals(right, pair.right);
	}

	@Override
	public int hashCode() {
		return Objects.hash(left, right);
	}
}
