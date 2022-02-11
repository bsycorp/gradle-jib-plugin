package com.bsycorp.gradle.jib;

import org.gradle.api.services.BuildService;
import org.gradle.api.services.BuildServiceParameters;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public abstract class NamedLockProvider implements BuildService<NamedLockProvider.Params> {

    public interface Params extends BuildServiceParameters {
    }

    private static final ConcurrentHashMap<String, Lock> locks = new ConcurrentHashMap<>();

    public static Lock getLock(String name) {
        return locks.computeIfAbsent(name, (key) -> {
            return new ReentrantLock();
        });
    }

}
