/*
 * @(#)IntArrayDeque.java
 * Copyright © 2021 The authors and contributors of JHotDraw. MIT License.
 */
package org.jhotdraw8.collection;

import org.jhotdraw8.annotation.NonNull;
import org.jhotdraw8.util.Preconditions;

import java.util.AbstractCollection;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Deque;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * IntArrayDeque.
 *
 * @author Werner Randelshofer
 */
public class IntArrayDeque extends AbstractCollection<Integer> implements Deque<Integer> {
    /**
     * The length of this array is always a power of 2.
     */
    private int[] elements;

    /**
     * Index of the element at the head of the deque.
     */
    private int head;

    /**
     * Index at which the next element would be added to the tail of the deque.
     */
    private int tail;

    /**
     * Creates a new instance with an initial capacity for 8 elements.
     */
    public IntArrayDeque() {
        this(8);
    }

    /**
     * Creates a new instance with the specified initial capacity rounded up
     * to the next strictly positive power of two.
     *
     * @param capacity initial capacity
     */
    public IntArrayDeque(int capacity) {
        if (capacity < 0) {
            throw new IllegalArgumentException("capacity=" + capacity);
        }
        int size = Integer.highestOneBit(capacity + capacity - 1);
        elements = new int[Math.max(size, 0)];
    }

    @Override
    public boolean add(Integer integer) {
        addLastInt(integer);
        return true;
    }

    @Override
    public void addFirst(Integer integer) {
        addFirstInt(integer);
    }

    /**
     * Inserts the specified element at the head of this deque.
     *
     * @param e the element to add
     */
    public void addFirstInt(int e) {
        //Note: elements.length is a power of two.
        head = (head - 1) & (elements.length - 1);
        elements[head] = e;
        if (head == tail) {
            grow(size() + 1);
        }
    }

    @Override
    public void addLast(Integer integer) {
        addLastInt(integer);
    }

    public void addLastAll(int[] array) {
        addLastAll(array, 0, array.length);
    }

    public void addLastAll(int[] array, int offset, int length) {
        grow(size() + length);

        int firstPart = elements.length - tail;
        if (tail >= head && firstPart >= length
                || head - tail > length) {
            System.arraycopy(array, offset, elements, tail, length);
            tail = (tail + length) & (elements.length - 1);
            return;
        }

        System.arraycopy(array, offset, elements, tail, firstPart);
        int secondPart = length - firstPart;
        System.arraycopy(array, offset + firstPart, elements, 0, secondPart);
        tail = secondPart;
    }

    /**
     * Inserts the specified element at the tail of this deque.
     *
     * @param e the element
     */
    public void addLastInt(int e) {
        elements[tail] = e;
        tail = (tail + 1) & (elements.length - 1);
        if (tail == head) {
            grow(size() + 1);
        }
    }

    public void clear() {
        if (head < tail) {
            Arrays.fill(elements, head, tail + 1, 0);
        } else {
            Arrays.fill(elements, 0, tail + 1, 0);
            Arrays.fill(elements, head, elements.length, 0);
        }
        this.head = this.tail = 0;
    }

    @Override
    public boolean contains(Object o) {
        if (o instanceof Integer) {
            return firstIndexOfInt((int) o) != -1;
        }
        return false;
    }

    @Override
    public Iterator<Integer> descendingIterator() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Integer element() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
        return getFirstInt();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof IntArrayDeque)) {
            return false;
        }
        IntArrayDeque that = (IntArrayDeque) o;
        if (this.size() != that.size()) {
            return false;
        }
        int thisMask = elements.length - 1;
        int thatMask = that.elements.length - 1;
        for (int i = this.head, j = that.head; i != this.tail; i = (i + 1) & thisMask, j = (j + 1) & thatMask) {
            if (this.elements[i] != that.elements[j]) {
                return false;
            }
        }
        return true;
    }

    public int firstIndexOfInt(int o) {
        if (tail < head) {
            for (int i = head; i < elements.length; i++) {
                if (o == (elements[i])) {
                    return i - head;
                }
            }
            for (int i = 0; i < tail; i++) {
                if (o == (elements[i])) {
                    return i + elements.length - head;
                }
            }
        } else {
            for (int i = head; i < tail; i++) {
                if (o == (elements[i])) {
                    return i - head;
                }
            }
        }
        return -1;
    }

    @Override
    public Integer getFirst() {
        return getFirstInt();
    }

    /**
     * @throws NoSuchElementException if queue is empty
     */
    public int getFirstInt() {
        if (head == tail) {
            throw new NoSuchElementException();
        }
        int result = elements[head];
        return result;
    }

    @Override
    public Integer getLast() {
        return getLastInt();
    }

    /**
     * @throws NoSuchElementException if queue is empty
     */
    public int getLastInt() {
        if (head == tail) {
            throw new NoSuchElementException();
        }
        int result = elements[tail == 0 ? elements.length - 1 : tail - 1];
        return result;
    }

    /**
     * Increases the capacity of this deque.
     */
    private void grow(int capacity) {
        if (elements.length >= capacity) {
            return;
        }
        //assert head == tail;
        int size = size();
        int p = head;
        int n = elements.length;
        int r = n - p; // number of elements to the right of p
        int newCapacity = Math.max(1, Integer.highestOneBit(capacity + capacity - 1));
        if (newCapacity < 0) {
            throw new IllegalStateException("Sorry, deque too big");
        }
        int[] a = new int[newCapacity];
        System.arraycopy(elements, p, a, 0, r);
        System.arraycopy(elements, 0, a, r, p);
        elements = a;
        head = 0;
        tail = size;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        int mask = elements.length - 1;
        for (int i = head; i != tail; i = (i + 1) & mask) {
            hash = hash * 31 + elements[i];
        }
        return hash;
    }

    /**
     * Returns true if this deque is empty.
     *
     * @return {@code true} if this deque contains no elements
     */
    public boolean isEmpty() {
        return head == tail;
    }

    public @NonNull Iterator<Integer> iterator() {
        return new DeqIterator();
    }

    public int lastIndexOfInt(int o) {
        if (tail < head) {
            for (int i = elements.length - 1; i >= head; i--) {
                if (o == (elements[i])) {
                    return i - head;
                }
            }
            for (int i = tail - 1; i >= 0; i--) {
                if (o == (elements[i])) {
                    return i + elements.length - head;
                }
            }
        } else {
            for (int i = tail - 1; i >= head; i--) {
                if (o == (elements[i])) {
                    return i - head;
                }
            }
        }
        return -1;
    }

    @Override
    public boolean offer(Integer integer) {
        addLastInt(integer);
        return true;
    }

    @Override
    public boolean offerFirst(Integer integer) {
        addFirstInt(integer);
        return true;
    }

    @Override
    public boolean offerLast(Integer integer) {
        addLastInt(integer);
        return true;
    }

    @Override
    public Integer peek() {
        if (isEmpty()) {
            return null;
        }
        return getFirstInt();
    }

    @Override
    public Integer peekFirst() {
        if (isEmpty()) {
            return null;
        }
        return getFirstInt();
    }

    @Override
    public Integer peekLast() {
        if (isEmpty()) {
            return null;
        }
        return getLastInt();
    }

    @Override
    public Integer poll() {
        if (isEmpty()) {
            return null;
        }
        return removeFirstInt();
    }

    @Override
    public Integer pollFirst() {
        if (isEmpty()) {
            return null;
        }
        return removeFirstInt();
    }

    @Override
    public Integer pollLast() {
        if (isEmpty()) {
            return null;
        }
        return removeLastInt();
    }

    @Override
    public Integer pop() {
        return removeFirstInt();
    }

    /**
     * Removes the element at the head of the deque.
     *
     * @throws NoSuchElementException if the queue is empty
     */
    public int popInt() {
        return removeFirstInt();
    }

    @Override
    public void push(Integer integer) {
        addFirstInt(integer);
    }

    /**
     * Inserts the specified element at the head of this deque.
     *
     * @param e the element to add
     */
    public void pushInt(int e) {
        addFirstInt(e);
    }

    @Override
    public Integer remove() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
        return removeFirstInt();
    }

    @Override
    public boolean remove(Object o) {
        return removeFirstOccurrence(o);
    }

    /**
     * Removes an element at the given array index.
     */
    public void removeAt(int i) {
        int size = size();
        Preconditions.checkIndex(i, size);
        if (tail < head) {
            if (head + i < elements.length) {
                if (i > 0) {
                    System.arraycopy(elements, head, elements, head + 1, i - 1);
                }
                elements[head] = 0;
                head = head == elements.length ? 0 : head + 1;
            } else {
                if (i < size - 1) {
                    System.arraycopy(elements, i - elements.length + head + 1, elements, i - elements.length + head, size - i);
                }
                elements[tail] = 0;
                tail = tail == 0 ? elements.length : tail - 1;
            }
        } else {
            if (i < size - 1) {
                System.arraycopy(elements, head + i + 1, elements, head + i, size - i);
            }
            elements[head + i] = 0;
            tail--;
        }
    }

    @Override
    public Integer removeFirst() {
        return removeFirstInt();
    }

    /**
     * Removes the element at the head of the deque.
     *
     * @throws NoSuchElementException if the queue is empty
     */
    public int removeFirstInt() {
        if (head == tail) {
            throw new NoSuchElementException();
        }
        int result = elements[head];
        elements[head] = 0;
        head = (head == elements.length - 1) ? 0 : head + 1;
        return result;
    }

    @Override
    public boolean removeFirstOccurrence(Object o) {
        if (o instanceof Integer) {
            return removeFirstOccurrenceInt((int) o);
        }
        return false;
    }

    public boolean removeFirstOccurrenceInt(int o) {
        int index = firstIndexOfInt(o);
        if (index != -1) {
            removeAt(index);
            return true;
        }
        return false;
    }

    @Override
    public Integer removeLast() {
        return removeLastInt();
    }

    /**
     * @throws NoSuchElementException {@inheritDoc}
     */
    public int removeLastInt() {
        if (head == tail) {
            throw new NoSuchElementException();
        }
        tail = (tail == 0) ? elements.length - 1 : tail - 1;
        int result = elements[tail];
        elements[tail] = 0;
        return result;
    }

    @Override
    public boolean removeLastOccurrence(Object o) {
        if (o instanceof Integer) {
            int index = lastIndexOfInt((int) o);
            if (index != -1) {
                removeAt(index);
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the number of elements in this deque.
     *
     * @return the number of elements in this deque
     */
    public int size() {
        return (tail - head) & (elements.length - 1);
    }

    public @NonNull String toString() {
        Iterator<Integer> it = iterator();
        if (!it.hasNext()) {
            return "[]";
        }

        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (; ; ) {
            Integer e = it.next();
            sb.append(e);
            if (!it.hasNext()) {
                return sb.append(']').toString();
            }
            sb.append(',').append(' ');
        }
    }

    private class DeqIterator implements Iterator<Integer> {
        /**
         * Tail recorded at construction, to stop
         * iterator and also to check for co-modification.
         */
        private final int fence = tail;
        /**
         * Index of element to be returned by subsequent call to next.
         */
        private int cursor = head;

        public boolean hasNext() {
            return cursor != fence;
        }

        public Integer next() {
            if (cursor == fence) {
                throw new NoSuchElementException();
            }
            int result = elements[cursor];
            // This check doesn't catch all possible co-modifications,
            // but does catch the ones that corrupt traversal
            if (tail != fence) {
                throw new ConcurrentModificationException();
            }
            cursor = (cursor + 1) & (elements.length - 1);
            return result;
        }


    }
}
