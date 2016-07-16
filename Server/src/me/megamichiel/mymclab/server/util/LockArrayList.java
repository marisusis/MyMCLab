package me.megamichiel.mymclab.server.util;

import java.util.ArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class LockArrayList<E> extends ArrayList<E> {

    private final Lock lock = new ReentrantLock();
    private boolean isLocked;

    @Override
    public void clear() {
        if (isLocked) super.clear();
        else {
            lock.lock();
            try {
                super.clear();
            } finally {
                lock.unlock();
            }
        }
    }

    @Override
    public <T> T[] toArray(T[] a) {
        if (isLocked) return super.toArray(a);
        lock.lock();
        try {
            return super.toArray(a);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void removeRange(int fromIndex, int toIndex) {
        if (isLocked) super.removeRange(fromIndex, toIndex);
        else {
            lock.lock();
            try {
                super.removeRange(fromIndex, toIndex);
            } finally {
                lock.unlock();
            }
        }
    }

    @Override
    public boolean add(E e) {
        if (isLocked) return super.add(e);
        lock.lock();
        try {
            return super.add(e);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int size() {
        if (isLocked) return super.size();
        lock.lock();
        try {
            return super.size();
        } finally {
            lock.unlock();
        }
    }

    public void copyTo(int pos, Object[] dest, int destPos, int length) {
        for (int i = 0; i < length; i++) dest[destPos + i] = get(pos + i);
    }

    public void toggleLock() {
        isLocked = !isLocked;
        if (isLocked) lock.lock();
        else lock.unlock();
    }
}
