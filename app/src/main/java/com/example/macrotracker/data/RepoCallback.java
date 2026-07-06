package com.example.macrotracker.data;

public interface RepoCallback<T> {
    void onSuccess(T result);
    void onError(Exception e);
}
