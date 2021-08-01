package castservice;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.rmi.ServerException;
import java.util.LinkedList;

public class Cast implements Serializable, Cloneable
{
    private static final long serialVersionUID = 21118000202108L;
    public long originatorUserId_;
    public long bondId_;
    public int side_;    // 0 - Buy, 1 - Sell
    public float price_;
    public int quantity_;
    public volatile int status_; // 1 - Active, 0 - Canceled
    public long[] targetUserIds_;

    public Cast(long originatorUserId,
                long bondId,
                int side,
                float price,
                int quantity,
                int status,
                long[] targetUserIds)
    {
        originatorUserId_ = originatorUserId;
        bondId_ = bondId;
        side_ = side;
        price_ = price;
        quantity_ = quantity;
        status_ = status;
        targetUserIds_ = targetUserIds;
    }

    /**
     * Do local (limited) fields validation:
     * - quantity_ - positive, -1 - undefined
     * - price - non negative, -1 - undefined
     * - rest numeric fields - positive
     * TODO: bondId format?
     * @return  true/false
     */
    boolean isValid()
    {
        boolean allTargetIdsValid = true;
        for(long id : targetUserIds_)
            if(!isValidTargetUserId(id))
            {
                allTargetIdsValid = false;
                break;
            }
        return isValidOriginatorUserId(originatorUserId_) &&
                isValidBondId(bondId_) &&
                isValidSide(side_) &&
                isValidPrice(price_) &&
                isValidQuantity(quantity_) &&
                allTargetIdsValid;
    }

    static boolean isValidTargetUserId(long targetUserId)
    {
        return targetUserId > 0;
    }

    static boolean isValidQuantity(int quantity)
    {
        return quantity > 0 || quantity == -1;
    }

    static boolean isValidPrice(float price)
    {
        return price > 0 || price == -1;
    }

    static boolean isValidOriginatorUserId(long originatorUserId)
    {
        return originatorUserId > 0;
    }

    static boolean isValidBondId(long bondId)
    {
        return bondId > 0;
    }

    static boolean isValidSide(int side)
    {
        return side == 0 || side == 1;
    }

    public static String createId(long originatorUserId, long bondId, int side)
    {
        return String.format("%d;%d;%d", originatorUserId, bondId, side);
    }

    /**
     * Returns unique id using [originatorUserId_, bondId_, side_]
     * @return unique id as String
     */
    public String id()
    {
        return createId(originatorUserId_, bondId_, side_);
    }

    @Override
    protected Object clone()
    {
        Object cast;
        try {
            cast = super.clone();
        } catch(CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
        return cast;
    }

    @Override
    public String toString()
    {
        return String.format("%s - %d;%d;%d", super.toString(),
                originatorUserId_, bondId_, side_);
    }

    public static Cast[] loadFromCsvFile(String path) throws IOException
    {
        LinkedList<Cast> casts = new LinkedList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line = br.readLine();
            while(line != null)
            {
                line = line.trim();
                if(line.length() == 0)
                    continue;
                String[] flds = line.split(",");
                if(flds.length != 7)
                    throw new ServerException(String.format("Invalid fields number: [%s]", line));
                String[] sTargetIds = flds[6].split(";");
                long[] targetIds = new long[sTargetIds.length];
                for(int i = 0; i < sTargetIds.length; i++)
                    targetIds[i] = Long.parseLong(sTargetIds[i]);
                casts.add(new Cast(
                        Long.parseLong(flds[0]),        // originatorUserId
                        Long.parseLong(flds[1]),        // bondId
                        Integer.parseInt(flds[2]),      // side
                        Long.parseLong(flds[3]),        // price
                        Integer.parseInt(flds[4]),      // quantity
                        Integer.parseInt(flds[5]),      // status
                        targetIds                       // targetUserIds
                ));
                line = br.readLine();
            }
        }
        return (Cast[])casts.toArray();
    }
}
