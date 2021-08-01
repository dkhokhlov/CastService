package castservice;

import java.io.ObjectInputStream;
import java.net.Socket;
import java.rmi.Naming;

import org.junit.Test;


public class TestSendCast {

    @Test
    public void Basic1() throws Exception
    {
        CastService service = new CastService();
        service.register();
        String host_addr = "localhost";
        String addr = String.format("rmi://%s:%d/%s", host_addr,
                ICastService.REGISTRY_PORT, ICastService.RMI_NAME);
        ICastService remote_service = (ICastService)Naming.lookup(addr);
    }


}
