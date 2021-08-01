package castservice;

import java.io.FileInputStream;
import java.util.Properties;
import java.rmi.Naming;
import java.rmi.RemoteException;

public class Client {
    private final ICastService service_;

    public Client(String host_addr) throws Exception
    {
        String addr = String.format("rmi://%s:%d/%s", host_addr,
                ICastService.REGISTRY_PORT, ICastService.RMI_NAME);
        service_ = (ICastService) Naming.lookup(addr);
    }

    public ICastService service()
    {
        return service_;
    }
    
    public static void main(String[] args) throws Exception
    {
        Cast a = new Cast(1L, 2L, 0, 3.0f, 4, 1, new long[] {10L, 11L, 12L, 13L});
        Cast b = new Cast(1L, 2L, 0, 3.0f, 4, 0, new long[] {10L, 11L, 12L, 13L});
        int exit_code = 0;
        Client client = new Client(args[0]);

        String ticket = client.service().sendCast(a);

        Cast[] casts = client.service().getActiveCasts(10);

        System.out.println("Exiting...");
    }
}


