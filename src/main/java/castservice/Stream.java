package castservice;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class Stream implements Runnable {
    final static int SO_TIMEOUT = 200; // ms

    // queue: outgoing (streaming) Casts associated with origId
    public final BlockingQueue<Cast> streamQueue_;
    public volatile boolean isShutdown_ = false;
    public Socket streamSocket_ = null;
    public int port_;
    public ServerSocket serverSocket_;
    List<Stream> target_streams_; // from Target, thread safe
    public long targetUserId_;
    Thread thread_;

    /////////////////////////////////////////////////

    Stream(long targetUserId, List<Stream> streams, int queueSize) throws IOException
    {
        targetUserId_ = targetUserId;
        streamQueue_ = new LinkedBlockingQueue<>(queueSize);
        target_streams_ = streams;
        serverSocket_ = new ServerSocket(0);
        port_ = serverSocket_.getLocalPort();
        serverSocket_.setSoTimeout(SO_TIMEOUT);
        serverSocket_.setPerformancePreferences(0, 2, 1);
        thread_ = new Thread(this);
        thread_.start();
    }

    public int getPort() { return port_; }

    protected void finalize() throws InterruptedException
    {
        shutdown();
    }

    public void shutdown() throws InterruptedException
    {
        System.out.printf("Stream shutdown: targetUserId=%d port=%d", targetUserId_, port_);
        isShutdown_ = true;
        try {
            if (serverSocket_ != null)
                serverSocket_.close();
        } catch(Exception ignore) {}
        try {
            if(streamSocket_ != null)
                streamSocket_.close();
        } catch(Exception ignore) {}
        thread_.join();
    }

    public void sendCast(Cast cast) throws ServiceErrorException
    {
        if(streamQueue_.remainingCapacity() < 2)
        {
            target_streams_.remove(this);
            throw new ServiceErrorException(String.format("streamQueue overrun: targetUserId=%d port=%d",
                    targetUserId_, port_));
        }
        streamQueue_.add(cast);
    }

    public void run()
    {
        try
        {
            System.out.printf("Stream waiting accept: targetUserId=%d port=%d",
                    targetUserId_, port_);
            // waiting for connection from client
            while (!isShutdown_) {
                try {
                    streamSocket_ = serverSocket_.accept();
                    serverSocket_.close(); // no more connections
                    break;
                } catch (SocketTimeoutException ignore) { }
            }
            System.out.printf("Stream connected: targetUserId=%d port=%d remote=%d",
                    targetUserId_, port_, streamSocket_.getPort());
            ObjectOutputStream oos = new ObjectOutputStream(streamSocket_.getOutputStream());
            oos.flush();
            while (!isShutdown_) {
                Cast cast;
                try {
                    cast = streamQueue_.poll(200, TimeUnit.MILLISECONDS);
                } catch (InterruptedException ignore) {
                    continue;
                }
                oos.writeObject(cast);
                oos.flush();
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        target_streams_.remove(this);
        System.out.printf("Stream thread exit: originatorUserId=%d port=%d", targetUserId_, port_);
    }
}
