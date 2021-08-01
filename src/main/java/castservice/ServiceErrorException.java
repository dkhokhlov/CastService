package castservice;

public class ServiceErrorException extends Exception {
    public ServiceErrorException(String msg) { super(msg); }
    public ServiceErrorException(Throwable e) { super(e); }
}
