package cn.kurt6.unknown_echoes.client;

import java.util.Arrays;

/** 距离只计算一次，按需取最近候选；昂贵的装备检查留到取出之后。 */
final class NearestFxCandidates<T> {
    private Object[] values;
    private double[] distances;
    private int[] orders;
    private int size;
    private boolean prepared;

    NearestFxCandidates(int capacity) {
        values = new Object[capacity];
        distances = new double[capacity];
        orders = new int[capacity];
    }

    void add(T value, double distance) {
        if (prepared) throw new IllegalStateException("selection already started");
        if (!Double.isFinite(distance) || distance < 0) return;
        if (size == values.length) {
            int capacity = Math.max(1, size * 2);
            values = Arrays.copyOf(values, capacity);
            distances = Arrays.copyOf(distances, capacity);
            orders = Arrays.copyOf(orders, capacity);
        }
        values[size] = value;
        distances[size] = distance;
        orders[size] = size;
        size++;
    }

    boolean isEmpty() {
        return size == 0;
    }

    @SuppressWarnings("unchecked")
    T removeNearest() {
        if (size == 0) throw new IllegalStateException("empty selection");
        if (!prepared) {
            for (int i = size / 2 - 1; i >= 0; i--) siftDown(i);
            prepared = true;
        }
        T value = (T) values[0];
        size--;
        values[0] = values[size];
        distances[0] = distances[size];
        orders[0] = orders[size];
        values[size] = null;
        if (size > 0) siftDown(0);
        return value;
    }

    private boolean precedes(int left, int right) {
        return distances[left] < distances[right]
                || distances[left] == distances[right] && orders[left] < orders[right];
    }

    private void siftDown(int index) {
        while (index * 2 + 1 < size) {
            int child = index * 2 + 1;
            if (child + 1 < size && precedes(child + 1, child)) child++;
            if (!precedes(child, index)) return;
            Object value = values[index];
            double distance = distances[index];
            int order = orders[index];
            values[index] = values[child]; distances[index] = distances[child]; orders[index] = orders[child];
            values[child] = value; distances[child] = distance; orders[child] = order;
            index = child;
        }
    }
}
