/*
 * @(#)IntCharArrayEnumerator.java
 * Copyright © 2022 The authors and contributors of JHotDraw. MIT License.
 */

package org.jhotdraw8.collection;

import org.jhotdraw8.annotation.Nullable;

/**
 * An integer enumerator/spliterator over a char array.
 */
public class IntCharArrayEnumerator extends AbstractIntEnumerator {
    private final int limit;
    private final char[] arrows;
    private int index;

    public IntCharArrayEnumerator(int lo, int hi, char[] arrows) {
        super(hi - lo, ORDERED | NONNULL | SIZED | SUBSIZED);
        limit = hi;
        index = lo;
        this.arrows = arrows;
    }

    @Override
    public boolean moveNext() {
        if (index < limit) {
            current = arrows[index++];
            return true;
        }
        return false;
    }

    @Override
    public long estimateSize() {
        return limit - index;
    }

    public @Nullable IntCharArrayEnumerator trySplit() {
        int lo = index, mid = (lo + limit) >>> 1;
        return (lo >= mid) ? null : // divide range in half unless too small
                new IntCharArrayEnumerator(lo, index = mid, arrows);
    }

}
