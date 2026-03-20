package com.tai.workflow.repository.impl;

import lombok.extern.slf4j.Slf4j;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe cache using WeakReference with automatic cleanup via ReferenceQueue.
 *
 * <p>This cache automatically removes entries when the referenced values are garbage collected,
 * preventing memory leaks from stale weak reference entries.
 *
 * @author zhanghaolong1989@163.com
 */
@Slf4j
public class WeakReferenceCache<K, V> {
    private final ConcurrentHashMap<K, KeyedWeakReference<K, V>> cache = new ConcurrentHashMap<>();
    private final ReferenceQueue<V> referenceQueue = new ReferenceQueue<>();

    /**
     * WeakReference that holds its key for cleanup purposes.
     */
    private static class KeyedWeakReference<K, V> extends WeakReference<V> {
        private final K key;

        KeyedWeakReference(K key, V value, ReferenceQueue<V> queue) {
            super(value, queue);
            this.key = key;
        }

        K getKey() {
            return key;
        }
    }

    /**
     * Stores a value in the cache with weak reference semantics.
     *
     * @param key the cache key
     * @param value the value to cache (will be weakly referenced)
     */
    public void put(K key, V value) {
        cleanupStaleEntries();
        cache.put(key, new KeyedWeakReference<>(key, value, referenceQueue));
    }

    /**
     * Retrieves a value from the cache.
     *
     * <p>This method also triggers cleanup of stale entries whose values have been garbage collected.
     *
     * @param key the cache key
     * @return the cached value, or null if not present or garbage collected
     */
    public V get(K key) {
        cleanupStaleEntries();
        KeyedWeakReference<K, V> weakRef = cache.get(key);
        if (Objects.nonNull(weakRef)) {
            V value = weakRef.get();
            if (value == null) {
                // Value was garbage collected, remove the stale entry
                cache.remove(key, weakRef);
            }
            return value;
        }
        return null;
    }

    /**
     * Cleans up cache entries whose values have been garbage collected.
     *
     * <p>This method polls the reference queue for collected references and removes
     * the corresponding entries from the cache.
     */
    @SuppressWarnings("unchecked")
    private void cleanupStaleEntries() {
        KeyedWeakReference<K, V> ref;
        while ((ref = (KeyedWeakReference<K, V>) referenceQueue.poll()) != null) {
            cache.remove(ref.getKey(), ref);
        }
    }
}
