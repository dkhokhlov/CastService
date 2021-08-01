package castservice;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

class Target {
    // map: Cast id -> Cast, active casts (references). used by getCasts().
    final int TCASTS_NUM_HINT = Integer.parseInt(System.getProperty("castservice.TCASTS_NUM_HINT",
            "100"));
    final int STREAM_QUEUE_SIZE_MAX = Integer.parseInt(System.getProperty("castservice.STREAM_QUEUE_SIZE_MAX",
            "100"));
    final int STREAMS_NUM_MAX = Integer.parseInt(System.getProperty("castservice.STREAMS_NUM_MAX",
            "1000"));  // streams per target

    // map: Cast id -> Cast, all active casts (references) in service. used by cancelCast.
    Map<String, Cast> activeCasts_ = new ConcurrentHashMap<>(TCASTS_NUM_HINT);
    public long targetUserId_;
    // "Stream" list: Stream
    List<Stream> streams_ = new CopyOnWriteArrayList<>();

    /////////////////////////////////////////////////////////

    public Target(long targetUserId)
    {
        targetUserId_ = targetUserId;
    }

    public void sendCast(Cast cast) throws ServiceErrorException
    {
        // add / replace Cast in target's book
        activeCasts_.put(cast.id(), cast);
        for(Stream stream: streams_)
            stream.sendCast(cast); // put into stream queue
    }

    /**
     * Create new Stream.
     * @return server port to connect. only one connection will be accepted.
     * @throws ServiceErrorException
     */
    public int streamCasts() throws ServiceErrorException
    {
        if(streams_.size() >= STREAMS_NUM_MAX)
            throw new ServiceErrorException(String.format("Too many stream clients: %d", streams_.size()));
        int port;
        try
        {
            Stream stream = new Stream(targetUserId_, streams_, STREAM_QUEUE_SIZE_MAX);
            port = stream.getPort();
            // stream is ready for connection (only one)
            // stream  has queue and thread to process it
            // stream will self remove from streams_ on any problem or disconnects
            streams_.add(stream);
        } catch(Exception e) {
            throw new ServiceErrorException(e);
        }
        return port;
    }
}

