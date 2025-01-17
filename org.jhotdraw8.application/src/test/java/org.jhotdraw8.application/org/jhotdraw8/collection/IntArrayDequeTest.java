/*
 * @(#)IntArrayDequeTest.java
 * Copyright © 2022 The authors and contributors of JHotDraw. MIT License.
 */

package org.jhotdraw8.collection;

import org.jhotdraw8.annotation.NonNull;

/**
 * Tests {@link IntArrayDeque}.
 */
public class IntArrayDequeTest extends AbstractIntSequencedCollectionTest {
    @Override
    protected @NonNull IntSequencedCollection newInstance() {
        return new IntArrayDeque();
    }
}
