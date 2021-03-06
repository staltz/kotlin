package kotlin

//
// NOTE THIS FILE IS AUTO-GENERATED by the GenerateStandardLib.kt
// See: https://github.com/JetBrains/kotlin/tree/master/libraries/stdlib
//

import kotlin.platform.*
import java.util.*

import java.util.Collections // TODO: it's temporary while we have java.util.Collections in js

/**
 * Returns a list containing only distinct elements from the given collection.
 * The elements in the resulting list are in the same order as they were in the source collection.
 */
public fun <T> Array<out T>.distinct(): List<T> {
    return this.toMutableSet().toList()
}

/**
 * Returns a list containing only distinct elements from the given collection.
 * The elements in the resulting list are in the same order as they were in the source collection.
 */
public fun BooleanArray.distinct(): List<Boolean> {
    return this.toMutableSet().toList()
}

/**
 * Returns a list containing only distinct elements from the given collection.
 * The elements in the resulting list are in the same order as they were in the source collection.
 */
public fun ByteArray.distinct(): List<Byte> {
    return this.toMutableSet().toList()
}

/**
 * Returns a list containing only distinct elements from the given collection.
 * The elements in the resulting list are in the same order as they were in the source collection.
 */
public fun CharArray.distinct(): List<Char> {
    return this.toMutableSet().toList()
}

/**
 * Returns a list containing only distinct elements from the given collection.
 * The elements in the resulting list are in the same order as they were in the source collection.
 */
public fun DoubleArray.distinct(): List<Double> {
    return this.toMutableSet().toList()
}

/**
 * Returns a list containing only distinct elements from the given collection.
 * The elements in the resulting list are in the same order as they were in the source collection.
 */
public fun FloatArray.distinct(): List<Float> {
    return this.toMutableSet().toList()
}

/**
 * Returns a list containing only distinct elements from the given collection.
 * The elements in the resulting list are in the same order as they were in the source collection.
 */
public fun IntArray.distinct(): List<Int> {
    return this.toMutableSet().toList()
}

/**
 * Returns a list containing only distinct elements from the given collection.
 * The elements in the resulting list are in the same order as they were in the source collection.
 */
public fun LongArray.distinct(): List<Long> {
    return this.toMutableSet().toList()
}

/**
 * Returns a list containing only distinct elements from the given collection.
 * The elements in the resulting list are in the same order as they were in the source collection.
 */
public fun ShortArray.distinct(): List<Short> {
    return this.toMutableSet().toList()
}

/**
 * Returns a list containing only distinct elements from the given collection.
 * The elements in the resulting list are in the same order as they were in the source collection.
 */
public fun <T> Iterable<T>.distinct(): List<T> {
    return this.toMutableSet().toList()
}

/**
 * Returns a sequence containing only distinct elements from the given sequence.
 * The elements in the resulting sequence are in the same order as they were in the source sequence.
 */
public fun <T> Sequence<T>.distinct(): Sequence<T> {
    return this.distinctBy { it }
}

/**
 * Returns a list containing only distinct elements from the given collection according to the [keySelector].
 * The elements in the resulting list are in the same order as they were in the source collection.
 */
public inline fun <T, K> Array<out T>.distinctBy(keySelector: (T) -> K): List<T> {
    val set = HashSet<K>()
    val list = ArrayList<T>()
    for (e in this) {
        val key = keySelector(e)
        if (set.add(key))
            list.add(e)
    }
    return list
}

/**
 * Returns a list containing only distinct elements from the given collection according to the [keySelector].
 * The elements in the resulting list are in the same order as they were in the source collection.
 */
public inline fun <K> BooleanArray.distinctBy(keySelector: (Boolean) -> K): List<Boolean> {
    val set = HashSet<K>()
    val list = ArrayList<Boolean>()
    for (e in this) {
        val key = keySelector(e)
        if (set.add(key))
            list.add(e)
    }
    return list
}

/**
 * Returns a list containing only distinct elements from the given collection according to the [keySelector].
 * The elements in the resulting list are in the same order as they were in the source collection.
 */
public inline fun <K> ByteArray.distinctBy(keySelector: (Byte) -> K): List<Byte> {
    val set = HashSet<K>()
    val list = ArrayList<Byte>()
    for (e in this) {
        val key = keySelector(e)
        if (set.add(key))
            list.add(e)
    }
    return list
}

/**
 * Returns a list containing only distinct elements from the given collection according to the [keySelector].
 * The elements in the resulting list are in the same order as they were in the source collection.
 */
public inline fun <K> CharArray.distinctBy(keySelector: (Char) -> K): List<Char> {
    val set = HashSet<K>()
    val list = ArrayList<Char>()
    for (e in this) {
        val key = keySelector(e)
        if (set.add(key))
            list.add(e)
    }
    return list
}

/**
 * Returns a list containing only distinct elements from the given collection according to the [keySelector].
 * The elements in the resulting list are in the same order as they were in the source collection.
 */
public inline fun <K> DoubleArray.distinctBy(keySelector: (Double) -> K): List<Double> {
    val set = HashSet<K>()
    val list = ArrayList<Double>()
    for (e in this) {
        val key = keySelector(e)
        if (set.add(key))
            list.add(e)
    }
    return list
}

/**
 * Returns a list containing only distinct elements from the given collection according to the [keySelector].
 * The elements in the resulting list are in the same order as they were in the source collection.
 */
public inline fun <K> FloatArray.distinctBy(keySelector: (Float) -> K): List<Float> {
    val set = HashSet<K>()
    val list = ArrayList<Float>()
    for (e in this) {
        val key = keySelector(e)
        if (set.add(key))
            list.add(e)
    }
    return list
}

/**
 * Returns a list containing only distinct elements from the given collection according to the [keySelector].
 * The elements in the resulting list are in the same order as they were in the source collection.
 */
public inline fun <K> IntArray.distinctBy(keySelector: (Int) -> K): List<Int> {
    val set = HashSet<K>()
    val list = ArrayList<Int>()
    for (e in this) {
        val key = keySelector(e)
        if (set.add(key))
            list.add(e)
    }
    return list
}

/**
 * Returns a list containing only distinct elements from the given collection according to the [keySelector].
 * The elements in the resulting list are in the same order as they were in the source collection.
 */
public inline fun <K> LongArray.distinctBy(keySelector: (Long) -> K): List<Long> {
    val set = HashSet<K>()
    val list = ArrayList<Long>()
    for (e in this) {
        val key = keySelector(e)
        if (set.add(key))
            list.add(e)
    }
    return list
}

/**
 * Returns a list containing only distinct elements from the given collection according to the [keySelector].
 * The elements in the resulting list are in the same order as they were in the source collection.
 */
public inline fun <K> ShortArray.distinctBy(keySelector: (Short) -> K): List<Short> {
    val set = HashSet<K>()
    val list = ArrayList<Short>()
    for (e in this) {
        val key = keySelector(e)
        if (set.add(key))
            list.add(e)
    }
    return list
}

/**
 * Returns a list containing only distinct elements from the given collection according to the [keySelector].
 * The elements in the resulting list are in the same order as they were in the source collection.
 */
public inline fun <T, K> Iterable<T>.distinctBy(keySelector: (T) -> K): List<T> {
    val set = HashSet<K>()
    val list = ArrayList<T>()
    for (e in this) {
        val key = keySelector(e)
        if (set.add(key))
            list.add(e)
    }
    return list
}

/**
 * Returns a sequence containing only distinct elements from the given sequence according to the [keySelector].
 * The elements in the resulting sequence are in the same order as they were in the source sequence.
 */
public fun <T, K> Sequence<T>.distinctBy(keySelector: (T) -> K): Sequence<T> {
    return DistinctSequence(this, keySelector)
}

/**
 * Returns a set containing all elements that are contained by both this set and the specified collection.
 */
public fun <T> Array<out T>.intersect(other: Iterable<T>): Set<T> {
    val set = this.toMutableSet()
    set.retainAll(other)
    return set
}

/**
 * Returns a set containing all elements that are contained by both this set and the specified collection.
 */
public fun BooleanArray.intersect(other: Iterable<Boolean>): Set<Boolean> {
    val set = this.toMutableSet()
    set.retainAll(other)
    return set
}

/**
 * Returns a set containing all elements that are contained by both this set and the specified collection.
 */
public fun ByteArray.intersect(other: Iterable<Byte>): Set<Byte> {
    val set = this.toMutableSet()
    set.retainAll(other)
    return set
}

/**
 * Returns a set containing all elements that are contained by both this set and the specified collection.
 */
public fun CharArray.intersect(other: Iterable<Char>): Set<Char> {
    val set = this.toMutableSet()
    set.retainAll(other)
    return set
}

/**
 * Returns a set containing all elements that are contained by both this set and the specified collection.
 */
public fun DoubleArray.intersect(other: Iterable<Double>): Set<Double> {
    val set = this.toMutableSet()
    set.retainAll(other)
    return set
}

/**
 * Returns a set containing all elements that are contained by both this set and the specified collection.
 */
public fun FloatArray.intersect(other: Iterable<Float>): Set<Float> {
    val set = this.toMutableSet()
    set.retainAll(other)
    return set
}

/**
 * Returns a set containing all elements that are contained by both this set and the specified collection.
 */
public fun IntArray.intersect(other: Iterable<Int>): Set<Int> {
    val set = this.toMutableSet()
    set.retainAll(other)
    return set
}

/**
 * Returns a set containing all elements that are contained by both this set and the specified collection.
 */
public fun LongArray.intersect(other: Iterable<Long>): Set<Long> {
    val set = this.toMutableSet()
    set.retainAll(other)
    return set
}

/**
 * Returns a set containing all elements that are contained by both this set and the specified collection.
 */
public fun ShortArray.intersect(other: Iterable<Short>): Set<Short> {
    val set = this.toMutableSet()
    set.retainAll(other)
    return set
}

/**
 * Returns a set containing all elements that are contained by both this set and the specified collection.
 */
public fun <T> Iterable<T>.intersect(other: Iterable<T>): Set<T> {
    val set = this.toMutableSet()
    set.retainAll(other)
    return set
}

/**
 * Returns a set containing all elements that are contained by this set and not contained by the specified collection.
 */
public fun <T> Array<out T>.subtract(other: Iterable<T>): Set<T> {
    val set = this.toMutableSet()
    set.removeAll(other)
    return set
}

/**
 * Returns a set containing all elements that are contained by this set and not contained by the specified collection.
 */
public fun BooleanArray.subtract(other: Iterable<Boolean>): Set<Boolean> {
    val set = this.toMutableSet()
    set.removeAll(other)
    return set
}

/**
 * Returns a set containing all elements that are contained by this set and not contained by the specified collection.
 */
public fun ByteArray.subtract(other: Iterable<Byte>): Set<Byte> {
    val set = this.toMutableSet()
    set.removeAll(other)
    return set
}

/**
 * Returns a set containing all elements that are contained by this set and not contained by the specified collection.
 */
public fun CharArray.subtract(other: Iterable<Char>): Set<Char> {
    val set = this.toMutableSet()
    set.removeAll(other)
    return set
}

/**
 * Returns a set containing all elements that are contained by this set and not contained by the specified collection.
 */
public fun DoubleArray.subtract(other: Iterable<Double>): Set<Double> {
    val set = this.toMutableSet()
    set.removeAll(other)
    return set
}

/**
 * Returns a set containing all elements that are contained by this set and not contained by the specified collection.
 */
public fun FloatArray.subtract(other: Iterable<Float>): Set<Float> {
    val set = this.toMutableSet()
    set.removeAll(other)
    return set
}

/**
 * Returns a set containing all elements that are contained by this set and not contained by the specified collection.
 */
public fun IntArray.subtract(other: Iterable<Int>): Set<Int> {
    val set = this.toMutableSet()
    set.removeAll(other)
    return set
}

/**
 * Returns a set containing all elements that are contained by this set and not contained by the specified collection.
 */
public fun LongArray.subtract(other: Iterable<Long>): Set<Long> {
    val set = this.toMutableSet()
    set.removeAll(other)
    return set
}

/**
 * Returns a set containing all elements that are contained by this set and not contained by the specified collection.
 */
public fun ShortArray.subtract(other: Iterable<Short>): Set<Short> {
    val set = this.toMutableSet()
    set.removeAll(other)
    return set
}

/**
 * Returns a set containing all elements that are contained by this set and not contained by the specified collection.
 */
public fun <T> Iterable<T>.subtract(other: Iterable<T>): Set<T> {
    val set = this.toMutableSet()
    set.removeAll(other)
    return set
}

/**
 * Returns a mutable set containing all distinct elements from the given collection.
 */
public fun <T> Array<out T>.toMutableSet(): MutableSet<T> {
    val set = LinkedHashSet<T>(mapCapacity(size()))
    for (item in this) set.add(item)
    return set
}

/**
 * Returns a mutable set containing all distinct elements from the given collection.
 */
public fun BooleanArray.toMutableSet(): MutableSet<Boolean> {
    val set = LinkedHashSet<Boolean>(mapCapacity(size()))
    for (item in this) set.add(item)
    return set
}

/**
 * Returns a mutable set containing all distinct elements from the given collection.
 */
public fun ByteArray.toMutableSet(): MutableSet<Byte> {
    val set = LinkedHashSet<Byte>(mapCapacity(size()))
    for (item in this) set.add(item)
    return set
}

/**
 * Returns a mutable set containing all distinct elements from the given collection.
 */
public fun CharArray.toMutableSet(): MutableSet<Char> {
    val set = LinkedHashSet<Char>(mapCapacity(size()))
    for (item in this) set.add(item)
    return set
}

/**
 * Returns a mutable set containing all distinct elements from the given collection.
 */
public fun DoubleArray.toMutableSet(): MutableSet<Double> {
    val set = LinkedHashSet<Double>(mapCapacity(size()))
    for (item in this) set.add(item)
    return set
}

/**
 * Returns a mutable set containing all distinct elements from the given collection.
 */
public fun FloatArray.toMutableSet(): MutableSet<Float> {
    val set = LinkedHashSet<Float>(mapCapacity(size()))
    for (item in this) set.add(item)
    return set
}

/**
 * Returns a mutable set containing all distinct elements from the given collection.
 */
public fun IntArray.toMutableSet(): MutableSet<Int> {
    val set = LinkedHashSet<Int>(mapCapacity(size()))
    for (item in this) set.add(item)
    return set
}

/**
 * Returns a mutable set containing all distinct elements from the given collection.
 */
public fun LongArray.toMutableSet(): MutableSet<Long> {
    val set = LinkedHashSet<Long>(mapCapacity(size()))
    for (item in this) set.add(item)
    return set
}

/**
 * Returns a mutable set containing all distinct elements from the given collection.
 */
public fun ShortArray.toMutableSet(): MutableSet<Short> {
    val set = LinkedHashSet<Short>(mapCapacity(size()))
    for (item in this) set.add(item)
    return set
}

/**
 * Returns a mutable set containing all distinct elements from the given collection.
 */
public fun <T> Iterable<T>.toMutableSet(): MutableSet<T> {
    return when (this) {
        is Collection<T> -> LinkedHashSet(this)
        else -> toCollection(LinkedHashSet<T>())
    }
}

/**
 * Returns a mutable set containing all distinct elements from the given sequence.
 */
public fun <T> Sequence<T>.toMutableSet(): MutableSet<T> {
    val set = LinkedHashSet<T>()
    for (item in this) set.add(item)
    return set
}

/**
 * Returns a set containing all distinct elements from both collections.
 */
public fun <T> Array<out T>.union(other: Iterable<T>): Set<T> {
    val set = this.toMutableSet()
    set.addAll(other)
    return set
}

/**
 * Returns a set containing all distinct elements from both collections.
 */
public fun BooleanArray.union(other: Iterable<Boolean>): Set<Boolean> {
    val set = this.toMutableSet()
    set.addAll(other)
    return set
}

/**
 * Returns a set containing all distinct elements from both collections.
 */
public fun ByteArray.union(other: Iterable<Byte>): Set<Byte> {
    val set = this.toMutableSet()
    set.addAll(other)
    return set
}

/**
 * Returns a set containing all distinct elements from both collections.
 */
public fun CharArray.union(other: Iterable<Char>): Set<Char> {
    val set = this.toMutableSet()
    set.addAll(other)
    return set
}

/**
 * Returns a set containing all distinct elements from both collections.
 */
public fun DoubleArray.union(other: Iterable<Double>): Set<Double> {
    val set = this.toMutableSet()
    set.addAll(other)
    return set
}

/**
 * Returns a set containing all distinct elements from both collections.
 */
public fun FloatArray.union(other: Iterable<Float>): Set<Float> {
    val set = this.toMutableSet()
    set.addAll(other)
    return set
}

/**
 * Returns a set containing all distinct elements from both collections.
 */
public fun IntArray.union(other: Iterable<Int>): Set<Int> {
    val set = this.toMutableSet()
    set.addAll(other)
    return set
}

/**
 * Returns a set containing all distinct elements from both collections.
 */
public fun LongArray.union(other: Iterable<Long>): Set<Long> {
    val set = this.toMutableSet()
    set.addAll(other)
    return set
}

/**
 * Returns a set containing all distinct elements from both collections.
 */
public fun ShortArray.union(other: Iterable<Short>): Set<Short> {
    val set = this.toMutableSet()
    set.addAll(other)
    return set
}

/**
 * Returns a set containing all distinct elements from both collections.
 */
public fun <T> Iterable<T>.union(other: Iterable<T>): Set<T> {
    val set = this.toMutableSet()
    set.addAll(other)
    return set
}

