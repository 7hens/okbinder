package cn.thens.okbinder2;

final class ErrorUtils {
    public static RuntimeException wrap(Throwable error) {
        if (error instanceof RuntimeException) {
            return (RuntimeException) error;
        }
        return new ErrorWrapper(error);
    }

    public static Throwable unwrap(Throwable error) {
        if (error instanceof ErrorWrapper) {
            return error.getCause();
        }
        return error;
    }

    private static final class ErrorWrapper extends RuntimeException {
        ErrorWrapper(Throwable cause) {
            super(cause);
        }
    }
}
