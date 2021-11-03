/*
 * @(#)DirectedGraphBuilder.java
 * Copyright © 2021 The authors and contributors of JHotDraw. MIT License.
 */
package org.jhotdraw8.graph;

import org.jhotdraw8.annotation.NonNull;
import org.jhotdraw8.annotation.Nullable;
import org.jhotdraw8.collection.IntEnumeratorSpliterator;
import org.jhotdraw8.util.TriFunction;

import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntConsumer;


/**
 * DirectedGraphBuilder.
 *
 * @param <V> the vertex data type
 * @param <A> the arrow data type
 * @author Werner Randelshofer
 */
public class SimpleMutableDirectedGraph<V, A> extends AbstractDirectedGraphBuilder
        implements MutableDirectedGraph<V, A>, AttributedIndexedDirectedGraph<V, A> {

    private static final Object TOMBSTONE_OBJECT = new Object();


    /**
     * Maps a vertex to a vertex index.
     */
    private final @NonNull Map<V, Integer> vertexMap;
    /**
     * Maps a vertex index to a vertex object.
     */
    private @NonNull List<V> vertices;
    /**
     * Maps an arrow index to an arrow object. May contain {@link #TOMBSTONE_OBJECT}s.
     */
    private final @NonNull List<Object> arrows;

    /**
     * Creates a new instance with an initial capacity for 16 vertices and 16 arrows.
     * <p>
     * Uses a non-identity hash map for storing the vertices.
     */
    public SimpleMutableDirectedGraph() {
        this(16, 16, false);
    }

    /**
     * Creates a new instance with the specified initial capacities.
     * <p>
     * Uses a non-identity hash map for storing the vertices.
     *
     * @param vertexCapacity the initial capacity for vertices
     * @param arrowCapacity  the initial capacity for arrows
     */
    public SimpleMutableDirectedGraph(int vertexCapacity, int arrowCapacity) {
        this(vertexCapacity, arrowCapacity, false);
    }

    /**
     * Creates a new instance with the specified initial capacities.
     *
     * @param vertexCapacity the initial capacity for vertices
     * @param arrowCapacity  the initial capacity for arrows
     * @param identityMap    whether to use an identity hash map for storing the vertices
     */
    public SimpleMutableDirectedGraph(int vertexCapacity, int arrowCapacity, boolean identityMap) {
        super(vertexCapacity, arrowCapacity);
        this.vertexMap = identityMap ? new IdentityHashMap<>(vertexCapacity) : new HashMap<>(vertexCapacity);
        this.vertices = new ArrayList<>(vertexCapacity);
        this.arrows = new ArrayList<>(arrowCapacity);
    }

    /**
     * Creates a new instance which contains a copy of the specified graph.
     *
     * @param graph a graph
     */
    public SimpleMutableDirectedGraph(@NonNull DirectedGraph<V, A> graph) {
        this(graph, Function.identity(), (v1, v2, a) -> a);
    }

    /**
     * Creates a new instance which contains a copy of the specified graph.
     * <p>
     * Uses a non-identity hash map for storing the vertices.
     *
     * @param graph        a graph
     * @param vertexMapper a mapping function for the vertices
     * @param arrowMapper  a mapping function for the arrows
     * @param <VV>         the vertex data type of the graph
     * @param <AA>         the arrow data type of the graph
     */
    public <VV, AA> SimpleMutableDirectedGraph(@NonNull DirectedGraph<VV, AA> graph,
                                               Function<VV, V> vertexMapper,
                                               TriFunction<VV, VV, AA, A> arrowMapper) {
        super(graph.getVertexCount(), graph.getArrowCount());
        final int vcount = graph.getVertexCount();
        this.vertexMap = new HashMap<>(vcount);
        this.vertices = new ArrayList<>(vcount);
        this.arrows = new ArrayList<>(graph.getArrowCount());

        for (VV vv : graph.getVertices()) {
            addVertex(vertexMapper.apply(vv));
        }
        for (VV vv : graph.getVertices()) {
            for (int j = 0, n = graph.getNextCount(vv); j < n; j++) {
                @NonNull VV next = graph.getNext(vv, j);
                addArrow(vertexMapper.apply(vv),
                        vertexMapper.apply(next),
                        arrowMapper.apply(vv, next, graph.getNextArrow(vv, j)));
            }
        }
    }

    /**
     * Adds a directed arrow from va to vb.
     *
     * @param va    vertex a
     * @param vb    vertex b
     * @param arrow the arrow
     */
    public void addArrow(@NonNull V va, @NonNull V vb, @Nullable A arrow) {
        Objects.requireNonNull(va, "va");
        Objects.requireNonNull(vb, "vb");
        int a = vertexMap.get(va);
        int b = vertexMap.get(vb);
        int arrowIndex = super.buildAddArrow(a, b);
        if (arrowIndex == arrows.size()) {
            arrows.add(arrow);
        } else {
            arrows.set(arrowIndex, arrow);
            cachedArrows = null;
        }
    }


    @Override
    public void removeArrow(@NonNull V v, @NonNull V u, @Nullable A a) {
        Integer vidx = vertexMap.get(v);
        int index = 0;
        for (IntEnumeratorSpliterator it = getNextVertices(vidx); it.moveNext(); ) {
            int uidx = it.current();
            if (u.equals(vertices.get(uidx)) && Objects.equals(a, getNextArrow(vidx, index))) {
                int indexOfRemovedArrow = buildRemoveArrowAt(vertexMap.get(v), index);
                arrows.set(indexOfRemovedArrow, TOMBSTONE_OBJECT);
                cachedArrows = null;
                return;
            }
            index++;
        }
    }

    @Override
    public void removeArrow(@NonNull V v, @NonNull V u) {
        Integer vidx = vertexMap.get(v);
        int index = 0;
        for (IntEnumeratorSpliterator it = getNextVertices(vidx); it.moveNext(); ) {
            int uidx = it.current();
            if (u.equals(vertices.get(uidx))) {
                int indexOfRemovedArrow = buildRemoveArrowAt(vertexMap.get(v), index);
                arrows.set(indexOfRemovedArrow, TOMBSTONE_OBJECT);
                cachedArrows = null;
                return;
            }
            index++;
        }
    }

    @Override
    public void removeArrowAt(@NonNull V v, int k) {
        int indexOfRemovedArrow = buildRemoveArrowAt(vertexMap.get(v), k);
        arrows.set(indexOfRemovedArrow, TOMBSTONE_OBJECT);
        cachedArrows = null;
    }

    /**
     * Adds an arrow from 'va' to 'vb' and an arrow from 'vb' to 'va'.
     *
     * @param va    vertex a
     * @param vb    vertex b
     * @param arrow the arrow
     */
    public void addBidiArrow(V va, V vb, A arrow) {
        addArrow(va, vb, arrow);
        addArrow(vb, va, arrow);
    }

    /**
     * Adds a vertex.
     *
     * @param v vertex
     */
    public void addVertex(@NonNull V v) {
        Objects.requireNonNull(v, "v");
        vertexMap.computeIfAbsent(v, addVertexIfAbsent);
    }

    /**
     * Adds a vertex at the specified index.
     *
     * @param v    vertex
     * @param vidx vertex index
     */
    public void addVertex(@NonNull V v, int vidx) {
        Objects.requireNonNull(v, "v");
        buildInsertVertexAt(vidx);
        vertices.add(vidx, v);
        for (Map.Entry<V, Integer> entry : vertexMap.entrySet()) {
            Integer uidx = entry.getValue();
            if (uidx >= vidx) {
                entry.setValue(uidx + 1);
            }
        }
        vertexMap.put(v, vidx);

        cachedArrows = null;
    }

    @Override
    public void removeVertex(@NonNull V v) {
        Integer vidxBox = vertexMap.remove(v);
        if (vidxBox == null) {
            return;
        }
        int vidx = (int) vidxBox;
        // Remove all outgoing vertices
        for (int i = getNextCount(vidx) - 1; i >= 0; i--) {
            int indexOfRemovedArrow = buildRemoveArrowAt(vidx, i);
            arrows.set(indexOfRemovedArrow, TOMBSTONE_OBJECT);
        }
        // Remove all incoming vertices
        for (int uidx = 0, n = getVertexCount(); uidx < n; uidx++) {
            for (int i = getNextCount(uidx) - 1; i >= 0; i--) {
                int next = getNext(uidx, i);
                if (next == vidx) {
                    int indexOfRemovedArrow = buildRemoveArrowAt(uidx, i);
                    arrows.set(indexOfRemovedArrow, TOMBSTONE_OBJECT);
                }
            }
        }
        buildRemoveVertexAfterArrowsHaveBeenRemoved(vidx);
        vertices.remove(vidx);
        for (Map.Entry<V, Integer> entry : vertexMap.entrySet()) {
            Integer uidx = entry.getValue();
            if (uidx > vidx) {
                entry.setValue(uidx - 1);
            }
        }

        cachedArrows = null;
    }

    /**
     * Performance: We need this lambda very often if a large graph is created
     * with this builder.
     */
    private final Function<V, Integer> addVertexIfAbsent = k -> {
        vertices.add(k);
        buildAddVertex();
        return vertices.size() - 1;
    };


    @Override
    public void clear() {
        super.clear();
        vertexMap.clear();
        vertices.clear();
        arrows.clear();
        cachedArrows = null;
    }

    @Override
    public @NonNull A getNextArrow(@NonNull V vertex, int index) {
        int arrowId = getNextArrowIndex(getVertexIndex(vertex), index);
        return (A) arrows.get(arrowId);
    }

    @Override
    public @NonNull V getNext(@NonNull V v, int i) {
        return getVertex(getNext(getVertexIndex(v), i));
    }

    @Override
    public int getNextCount(@NonNull V v) {
        return getNextCount(getVertexIndex(v));
    }

    @Override
    public V getVertex(int vi) {
        if (vertices.get(vi) == null) {
            System.err.println("DIrectedGraphBuilder is broken");
        }
        return vertices.get(vi);
    }

    @Override
    public int getVertexIndex(V v) {
        Integer index = vertexMap.get(v);
        return index;
    }

    public A getArrow(int index) {
        // This has quadratic performance!
        int i = index;
        int vidx = 0;
        int nextCount = getNextCount(vidx);
        while (i >= nextCount) {
            vidx++;
            i -= nextCount;
            nextCount = getNextCount(vidx);
        }
        return getNextArrow(vidx, i);
    }

    @Override
    @SuppressWarnings("unchecked")
    public A getNextArrow(int vi, int i) {
        int arrowId = getNextArrowIndex(vi, i);
        return (A) arrows.get(arrowId);
    }

    private List<A> cachedArrows;

    @Override
    public @NonNull Collection<A> getArrows() {
        if (cachedArrows == null) {
            List<A> list = new ArrayList<>();
            for (Object arrow : arrows) {
                if (arrow != TOMBSTONE_OBJECT) {
                    list.add((A) arrow);
                }
            }
            cachedArrows = Collections.unmodifiableList(list);
        }
        return cachedArrows;
    }

    @Override
    public @NonNull Set<V> getVertices() {
        class VertexIterator implements Iterator<V> {

            private int index;
            private final int vertexCount;

            public VertexIterator() {
                vertexCount = getVertexCount();
            }

            @Override
            public boolean hasNext() {
                return index < vertexCount;
            }

            @Override
            public V next() {
                return getVertex(index++);
            }

        }
        return new AbstractSet<V>() {
            @Override
            public @NonNull Iterator<V> iterator() {
                return new VertexIterator();
            }

            @Override
            public boolean contains(Object o) {
                return vertices.contains(o);
            }

            @Override
            public int size() {
                return getVertexCount();
            }

        };
    }

    public @NonNull Collection<V> getNextVertices(@NonNull V vertex) {
        class NextVertexIterator implements Spliterator<V> {
            private final Spliterator.OfInt it;

            NextVertexIterator(Spliterator.OfInt nextVertices) {
                this.it = nextVertices;
            }

            @Override
            public boolean tryAdvance(Consumer<? super V> action) {
                return it.tryAdvance((IntConsumer) i -> action.accept(getVertex(i)));
            }

            @Override
            public Spliterator<V> trySplit() {
                OfInt splitted = it.trySplit();
                return splitted == null ? null : new NextVertexIterator(splitted);
            }

            @Override
            public long estimateSize() {
                return it.estimateSize();
            }

            @Override
            public int characteristics() {
                return it.characteristics();
            }
        }
        return new AbstractCollection<V>() {
            @Override
            public @NonNull Spliterator<V> spliterator() {
                return new NextVertexIterator(getNextVertices(getVertexIndex(vertex)));
            }

            @Override
            public Iterator<V> iterator() {
                return Spliterators.iterator(spliterator());
            }

            @Override
            public int size() {
                return getNextCount(vertex);
            }
        };
    }


}