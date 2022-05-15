package com.ptsmods.asmremapper.util;

import java.util.Objects;

/**
 * Simple class to hold two values that are related to each other.
 * @param left The first value
 * @param right The second value
 * @param <L> The type of the first value
 * @param <R> The type of the second value
 */
public record Pair<L, R>(L left, R right) {
	/**
	 * Construct a new Pair
	 * @param left The first value
	 * @param right The second value
	 * @return A new pair containing both passed values
	 * @param <L> The type of the first value
	 * @param <R> The type of the second value
	 */
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
