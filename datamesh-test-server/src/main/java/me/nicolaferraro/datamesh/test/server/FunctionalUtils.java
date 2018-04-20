package me.nicolaferraro.datamesh.test.server;

final class FunctionalUtils {

    private FunctionalUtils() {
    }

    static void ensure(ThrowingSupplier<Boolean> call) {
        try {
            if (!call.get()) {
                throw new RuntimeException("Operation failed with wrong response code");
            }
        } catch (Exception ex) {
            throw new RuntimeException("Operation failed with exception", ex);
        }
    }

    static <T> T wrap(ThrowingSupplier<T> call) {
        try {
            return call.get();
        } catch (Exception ex) {
            throw new RuntimeException("Operation failed with exception", ex);
        }
    }

    static void wrap(ThrowingRunnable runnable) {
        try {
            runnable.run();
        } catch (Exception ex) {
            throw new RuntimeException("Operation failed with exception", ex);
        }
    }

    @FunctionalInterface
    interface ThrowingSupplier<T> {
        T get() throws Exception;
    }

    @FunctionalInterface
    interface ThrowingRunnable {
        void run() throws Exception;
    }

}
