package in.satpathy.charanna.broker.zerodha;
import com.zerodhatech.kiteconnect.kitehttp.SessionExpiryHook;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *  Only logs the event to the logger.
 */
public class CharannaSessionExpiryHook implements SessionExpiryHook  {

    private static final Logger logger = LogManager.getLogger( CharannaSessionExpiryHook.class ) ;

    /**
     *  Default Constructor.
     */
    public CharannaSessionExpiryHook() {
    }

    @Override
    public void sessionExpired() {
        logger.info( "KiteConnect Session Expired." ) ;
    }

}   /*  End of the CharannaSessionExpiryHook class. */
