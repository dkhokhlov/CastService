package castservice;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.Socket;
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
        if(args.length < 1)
        {
            System.err.println("Argument missing: java castservice.Main <service host address>");
            System.exit(2);
        }
        String service_host = args[0];
        Cast a = new Cast(1L, 2L, 0, 3.0f, 4, 1, new long[] {10L, 11L, 12L, 13L});
        Cast b = new Cast(1L, 2L, 0, 3.0f, 4, 0, new long[] {10L, 11L, 12L, 13L});
        int exit_code = 0;
        Client client = new Client(service_host);

        client.service().sendCast(a);
        Cast[] casts = client.service().getActiveCasts(10);
        assert(casts.length == 1 && casts[0].id().equals(a.id()));
        casts = client.service().getActiveCasts(11);
        assert(casts.length == 1 && casts[0].id().equals(a.id()));

        client.service().cancelCast(a.originatorUserId_, a.bondId_, a.side_);
        casts = client.service().getActiveCasts(10);
        assert(casts.length == 0);
        casts = client.service().getActiveCasts(11);
        assert(casts.length == 0);

        client.service().sendCast(a);
        client.service().sendCast(b);
        casts = client.service().getActiveCasts(10);
        assert(casts.length == 0);

        // streaming
        int port = client.service().streamCasts(10);
        Socket sock = new Socket(service_host, port);
        ObjectInputStream stream = new ObjectInputStream(sock.getInputStream());
        client.service().sendCast(a);
        for(int i = 1; i < 10; i++)
        {
            Cast cast = (Cast)stream.readObject();
            System.out.println(cast);
            client.service().sendCast(a);
            Thread.sleep(1000);
        }
        System.out.println("Exiting...");
    }
}


