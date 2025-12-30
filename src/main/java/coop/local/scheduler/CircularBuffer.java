package coop.local.scheduler;

import java.util.ArrayList;
import java.util.List;

public class CircularBuffer<T> {

    private List<T> buffer = new ArrayList<>();
    private int currentIdx = -1;

    public void add(T data) {
        if(buffer.isEmpty()) {
            currentIdx = 0;
        }

        buffer.add(data);
    }

    public T remove() {
        if(buffer.isEmpty()) {
            return null;
        }

        T tmp = buffer.get(currentIdx);
        buffer.remove(currentIdx);
        if(currentIdx >= buffer.size()) {
            currentIdx = 0;
        }

        if(buffer.isEmpty()) {
            currentIdx = -1;
        }

        return tmp;
    }

    public T next() {
        if(buffer.isEmpty()) {
            return null;
        }

        int nextIdx = currentIdx + 1;
        if(nextIdx >= buffer.size()) {
            nextIdx = 0;
        }

        currentIdx = nextIdx;
        return buffer.get(currentIdx);
    }
}
