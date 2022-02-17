/*
 * @(#)IntImmutableDirectedGraph.java
 * Copyright © 2021 The authors and contributors of JHotDraw. MIT License.
 */
package org.jhotdraw8.graph;

import org.jhotdraw8.annotation.NonNull;
import org.jhotdraw8.annotation.Nullable;
import org.jhotdraw8.collection.AbstractIntEnumeratorSpliterator;
import org.jhotdraw8.collection.IntEnumeratorSpliterator;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * ImmutableIntDirectedGraph.
 *
 * @param <V> the vertex data type
 * @param <A> the arrow data type
 * @author Werner Randelshofer
 */
public class ImmutableIntIndexedDirectedGraph<V, A> implements AttributedIndexedDirectedGraph<V, A>, DirectedGraph<V, A> {

    /**
     * Holds the indices to the next vertices.
     * <p>
     * The indices are stored in consecutive runs for each vertex,
     * starting at the offset given by {@code nextOffset}.
     * <p>
     * Given vertex index {@code vi < nextOffset.length - 1}<br>
     * then<br>
     * {@code offset = nextOffset[vi]}
     * {@code count = nextOffset[vi+1] - offset}
     * <p>
     * Given vertex index {@code vi == nextOffset.length - 1}<br>
     * then<br>
     * {@code offset = nextOffset[vi]}
     * {@code count = nextOffset.length - offset}
     */
    protected final @NonNull int[] next;

    /**
     * Holds offsets into the {@link #next} table and the
     * {@link #nextArrows} table.
     * <p>
     * Given vertex index {@code vi},<br>
     * {@code nextOffset[vi]} yields the offset {@code ai}
     * in the tables {@link #next} table and the {@link #nextArrows}.
     * <p>
     * Given vertex index {@code vi < nextOffset.length - 1},<br>
     * {@code nextOffset[vi+1]) - nextOffset[vi]} yields the
     * number of outgoing arrows of that vertex.
     * <p>
     * Given vertex index {@code vi == nextOffset.length - 1},<br>
     * {@code nextOffset.length - nextOffset[vi]} yields the
     * number of outgoing arrows of that vertex.
     */
    protected final @NonNull int[] nextOffset;

    /**
     * Holds the arrow objects.
     * <p>
     * The arrows are stored in consecutive runs for each vertex,
     * starting at the offset given by {@code nextOffset}.
     * <p>
     * See {@link #next}.
     */
    protected final @NonNull A[] nextArrows;
    /**
     * Holds the vertex objects.
     * <p>
     * Given vertex index {@code vi},<br>
     * {@code vertices[vi|} yields the vertex {@code v}.
     */
    protected final @NonNull V[] vertices;
    /**
     * Maps vertices the vertex indices.
     * <p>
     * Given vertex {@code v},<br>
     * {@code vertexToIndexMap.get(v)} yields the vertex index {@code vi}.
     */
    protected final @NonNull Map<V, Integer> vertexToIndexMap;

    /**
     * Creates a new instance from the specified graph.
     *
     * @param graph a graph
     */
    public ImmutableIntIndexedDirectedGraph(@NonNull AttributedIndexedDirectedGraph<V, A> graph) {

        final int arrowCount = graph.getArrowCount();
        final int vertexCount = graph.getVertexCount();

        this.next = new int[arrowCount];

        @SuppressWarnings("unchecked")
        A[] uncheckedArrows = (A[]) new Object[arrowCount];
        this.nextArrows = uncheckedArrows;
        this.nextOffset = new int[vertexCount];
        @SuppressWarnings("unchecked")
        V[] uncheckedVertices = (V[]) new Object[vertexCount];
        this.vertices = uncheckedVertices;
        this.vertexToIndexMap = new HashMap<>(vertexCount);

        int offset = 0;
        for (int vi = 0; vi < vertexCount; vi++) {
            nextOffset[vi] = offset;
            V v = graph.getVertex(vi);
            this.vertices[vi] = v;
            vertexToIndexMap.put(v, vi);
            for (int i = 0, n = graph.getNextCount(vi); i < n; i++) {
                next[offset] = graph.getNext(vi, i);
                this.nextArrows[offset] = graph.getNextArrow(vi, i);
                offset++;
            }
        }
    }

    /**
     * Creates a new instance from the specified graph.
     *
     * @param graph a graph
     */
    public ImmutableIntIndexedDirectedGraph(@NonNull DirectedGraph<V, A> graph) {

        final int arrowCapacity = graph.getArrowCount();
        final int vertexCapacity = graph.getVertexCount();

        this.next = new int[arrowCapacity];
        @SuppressWarnings("unchecked")
        A[] uncheckedArrows = (A[]) new Object[arrowCapacity];
        this.nextArrows = uncheckedArrows;
        this.nextOffset = new int[vertexCapacity];
        @SuppressWarnings("unchecked")
        V[] uncheckedVertices = (V[]) new Object[vertexCapacity];
        this.vertices = uncheckedVertices;
        this.vertexToIndexMap = new HashMap<>(vertexCapacity);

        //    Map<V, Integer> vertexToIndexMap = new HashMap<>(vertexCapacity);
        {
            int vi = 0;
            for (V v : graph.getVertices()) {
                vertexToIndexMap.put(v, vi);
                vi++;
            }
        }

        {
            int offset = 0;
            int vi = 0;
            for (V v : graph.getVertices()) {

                nextOffset[vi] = offset;
                this.vertices[vi] = v;
                for (Arc<V, A> arc : graph.getNextArcs(v)) {
                    next[offset] = vertexToIndexMap.get(arc.getEnd());
                    nextArrows[offset] = arc.getArrow();
                    offset++;
                }
                vi++;
            }
        }
    }


    @Override
    public @NonNull A getArrow(int index) {
        return nextArrows[index];
    }

    @Override
    public @NonNull A getNextArrow(int vi, int i) {
        if (i < 0 || i >= getNextCount(vi)) {
            throw new IllegalArgumentException("i(" + i + ") < 0 || i >= " + getNextCount(vi));
        }
        return nextArrows[nextOffset[vi] + i];
    }

    @Override
    public @NonNull A getNextArrow(@NonNull V v, int i) {
        return getNextArrow(getVertexIndex(v), i);
    }

    @Override
    public int getArrowCount() {
        return next.length;
    }

    @Override
    public int getNext(int vidx, int i) {
        if (i < 0 || i >= getNextCount(vidx)) {
            throw new IllegalArgumentException("i(" + i + ") < 0 || i >= " + getNextCount(vidx));
        }
        return next[nextOffset[vidx] + i];
    }

    @Override
    public @NonNull V getNext(@NonNull V vertex, int i) {
        return vertices[getNext(vertexToIndexMap.get(vertex), i)];
    }

    protected int getArrowIndex(int vi, int i) {
        if (i < 0 || i >= getNextCount(vi)) {
            throw new IllegalArgumentException("i(" + i + ") < 0 || i >= " + getNextCount(vi));
        }
        return nextOffset[vi] + i;
    }

    @Override
    public int getNextCount(int vidx) {
        final int offset = nextOffset[vidx];
        final int nextOffset = (vidx == this.nextOffset.length - 1) ? this.next.length : this.nextOffset[vidx + 1];
        return nextOffset - offset;
    }

    @Override
    public int getVertexCount() {
        return nextOffset.length;
    }

    @Override
    public @NonNull V getVertex(int index) {
        return vertices[index];
    }

    @Override
    public int getVertexIndex(V vertex) {
        Integer index = vertexToIndexMap.get(vertex);
        return index == null ? -1 : index;
    }


    @Override
    public int getNextCount(@NonNull V vertex) {
        return getNextCount(vertexToIndexMap.get(vertex));
    }

    @Override
    public @NonNull Set<V> getVertices() {
        return Collections.unmodifiableSet(vertexToIndexMap.keySet());

    }

    @Override
    public @NonNull Collection<A> getArrows() {
        return Arrays.asList(nextArrows);
    }


    public @NonNull A getArrow(int vertex, int index) {
        return nextArrows[getArrowIndex(vertex, index)];
    }

    @Override
    public @NonNull IntEnumeratorSpliterator nextVerticesSpliterator(int vi) {
        class MySpliterator extends AbstractIntEnumeratorSpliterator {
            private int index;
            private int limit;
            private final int[] array;

            public MySpliterator(int lo, int hi, int[] nextVertices) {
                super(hi - lo, ORDERED | NONNULL | SIZED | SUBSIZED);
                limit = hi;
                index = lo;
                this.array = nextVertices;
            }

            @Override
            public boolean moveNext() {
                if (index < limit) {
                    current = array[index++];
                    return true;
                }
                return false;
            }

            public @Nullable MySpliterator trySplit() {
                int hi = limit, lo = index, mid = (lo + hi) >>> 1;
                return (lo >= mid) ? null : // divide range in half unless too small
                        new MySpliterator(lo, index = mid, array);
            }

        }
        final int offset = nextOffset[vi];
        final int nextOffset = (vi == this.nextOffset.length - 1) ? this.next.length : this.nextOffset[vi + 1];
        return new MySpliterator(offset, nextOffset, this.next);
    }

}