package com.github.chemikadze.pumploader;

import android.app.AlertDialog;
import android.content.Context;
import android.widget.NumberPicker;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class Utils {
    // why standard is not visible by compiler?
    final static NumberPicker.Formatter twoDigitFormatter = new NumberPicker.Formatter() {
        final StringBuilder mBuilder = new StringBuilder();
        final java.util.Formatter mFmt = new java.util.Formatter(mBuilder, java.util.Locale.US);
        final Object[] mArgs = new Object[1];
        public String format(int value) {
            mArgs[0] = value;
            mBuilder.delete(0, mBuilder.length());
            mFmt.format("%02d", mArgs);
            return mFmt.toString();
        }
    };

    static String formatElapsed(int seconds) {
        StringBuilder sb = new StringBuilder(8);
        sb.append(twoDigitFormatter.format(seconds / 60 / 60));
        sb.append(':');
        sb.append(twoDigitFormatter.format(seconds / 60 % 60));
        sb.append(':');
        sb.append(twoDigitFormatter.format(seconds % 60));
        return sb.toString();
    }

    static void errorDialog(Context context, String message) {
        new AlertDialog.Builder(context)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    static abstract class CompletedFuture<T> implements Future<T> {
        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return false;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public boolean isDone() {
            return true;
        }

        @Override
        public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return get();
        }
    }

    static class Success<T> extends CompletedFuture<T> {
        T value;

        Success(T value) { this.value = value; }

        @Override
        public T get() throws InterruptedException, ExecutionException { return this.value; }
    }

    static class Failure<T> extends CompletedFuture<T> {
        ExecutionException e;
        Failure(ExecutionException e) { this.e = e; }
        @Override
        public T get() throws InterruptedException, ExecutionException { throw e; }
    }

    abstract static class Function1<T, R> {
        abstract R apply(T argument);
    }
}
