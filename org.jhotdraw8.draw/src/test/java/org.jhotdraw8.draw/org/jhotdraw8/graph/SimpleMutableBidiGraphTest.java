package org.jhotdraw8.graph;

public class SimpleMutableBidiGraphTest extends AbstractMutableBidiGraphTest {
    @Override
    protected MutableBidiGraph<Integer, Character> newInstance() {
        return new SimpleMutableBidiGraph<>();
    }

    @Override
    protected MutableBidiGraph<Integer, Character> newInstance(DirectedGraph<Integer, Character> g) {
        return new SimpleMutableBidiGraph<>(g);
    }
}