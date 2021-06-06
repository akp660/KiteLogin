/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2021 Gautam Satpathy
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package in.satpathy.charanna.broker.zerodha;

/*
 *  Imports
 */
import com.zerodhatech.kiteconnect.KiteConnect;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.zerodhatech.models.Holding;
import com.zerodhatech.models.Profile;
import com.zerodhatech.models.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *  <P>Log in to Zerodha's Kite Connect.</P>
 *  <P>Needs: API Key, API Secret, (Zerodha) User ID, User Password, 2FA PIN and Redirect URL.</P>
 *  <P>Will look for a text file named "access.token.txt" in the folder from which it is run.
 *  If one is found the the access_token will be read from it and a connection established.</P>
 *  <P>If the text file is not found then it will access the Kite Login URL, fill out the
 *  Web Page form with the provided User ID & Password and submit the form. The 2FA PIN will
 *  be submitted in the response page and response_token retrieved to log into Kite Connect API
 *  servers. The Access Token will be written to file for later use.</P>
 *  <P>If the access token is found in the text file but is no longer valid, then the same process
 *  will be used to automatically log in and generate a new Access Token, which will then be
 *  written to file for later use.</P>
 *
 *  <P>After successful log in, the{@link KiteConnect} object available from this class will be
 *  authorized and can be used to </P>
 *
 *  @author Gautam Satpathy
 *  @version 0.1
 *  @date 2021-06.06
 *
 */
public class KiteLogin {

    static {
        //  We need the FireFox WebDriver for Selenium
        System.setProperty( "webdriver.gecko.driver", System.getProperty("user.dir") + File.separator + "geckodriver.exe" ) ;
    }

    private static final Logger logger = LogManager.getLogger( KiteLogin.class ) ;

    //  The file where we store the Access Token.
    private final String accessTokenFileName = System.getProperty("user.dir") + File.separator + "access.token.txt" ;


    //  The kite connect object
    private KiteConnect             kiteConnect ;

    /**
     *  Constructor.
     *
     *  @param userId
     *  @param password
     *  @param twoFactorPin
     *  @param apiKey
     *  @param apiSecret
     *  @param redirectUrl
     */
    public KiteLogin( String userId, String password, String twoFactorPin,
                      String apiKey, String apiSecret, String redirectUrl ) throws RuntimeException {
        executeKiteWebLogin( userId, password, twoFactorPin, apiKey, apiSecret, redirectUrl ) ;
    }

    /**
     *  Logs out of KiteConnect. The access token file will be deleted.
     *
     *  @return false if there was a problem.
     */
    public boolean logout()  {
        boolean status = false ;

        try {
            JSONObject resp = kiteConnect.logout();

            //  Delete the access token file.
            new File( accessTokenFileName ).delete() ;

            //  We are good.
            status = true ;

            logger.info( resp ) ;
            logger.info( "Logged out of Kite Connect." ) ;
        }
        catch (KiteException e) {
            logger.error( "KiteException while logging out.", e ) ;
        }
        catch (IOException e) {
            logger.error( "IOException while logging out.", e ) ;
        }

        return status ;
    }

    /**
     *
     *  @return
     */
    public KiteConnect getKiteConnect() {
        return kiteConnect;
    }

    /* ************************************************************************
     *  Helper methods.
     *************************************************************************/

    /**
     *  Execute the Kite Connect login process in it's entirety.
     */
    private void executeKiteWebLogin( String userId, String password, String twoFactorPin,
                                      String apiKey, String apiSecret, String redirectUrl ) {
        logger.info( "Starting KiteConnect.." ) ;
        String          accessToken ;

        //  Create the KiteConnect object for later use.
        if ( userId != null && password != null && twoFactorPin != null &&
             apiKey != null && apiSecret != null && redirectUrl != null )  {
            kiteConnect = new KiteConnect( apiKey ) ;
            kiteConnect.setUserId( userId ) ;
            kiteConnect.setSessionExpiryHook( new CharannaSessionExpiryHook() ) ;
        }
        else {
            throw new RuntimeException( "Don't have required data to log into Kite Connect. Cannot proceed." ) ;
        }

        //
        //  From here on we have a valid KiteConnect object but are not sure if we have a
        //  valid log in active yet. We test for that and if that fails, we perform a web
        //  login of the Kite web site using user id, password and 2FA Pin.
        //

        //  Load the access Token if it exists.
        accessToken = loadText( accessTokenFileName ) ;

        // Check if we already have a valid session, that is, we have a access_token that is valid.
        Profile profile = null ;
        if ( accessToken != null )  {
            kiteConnect.setAccessToken( accessToken ) ;
            try {
//                Routes routes = new Routes() ;
//                KiteRequestHandler handler = new KiteRequestHandler( null ) ;
//                handler.getRequest( routes.get("user.profile"), apiKey, accessToken ) ;
                profile = kiteConnect.getProfile() ;
                logger.info( "We have a existing valid session." ) ;
            }
            catch (IOException e) {
                logger.error( "IOException trying to get Profile to test for valid session.", e ) ;
            }
            catch (KiteException e) {
                logger.error( "KiteException trying to get Profile to test for valid session.", e ) ;
            }
        }

        if ( profile == null )  {
            //  We don't have a valid session. Login.
            try {
                String requestToken = autoWebLogin( userId, password, twoFactorPin, redirectUrl ) ;

                //  Start the User Session and get a User object.
                //  This will set the Access Token in the KiteConnect object.
                User user = getUserWithSession( requestToken, apiSecret ) ;
                logger.info( "[User] User Name: " + user.userName +
                        ", User ID: " + user.userId +
                        ", Login Time: " + user.loginTime +
                        ", Access Token: " + user.accessToken +
                        ", API Key: " + user.apiKey +
                        ", Public Token: " + user.publicToken +
                        ", Refresh Token: " + user.refreshToken +
                        ", Order Types: " + user.orderTypes +
                        ", Products: " + user.products +
                        ", Short Name: " + user.shortName +
                        ", User Type: " + user.userType
                ) ;

                //  Extract the details from the User object for later use.
                accessToken = user.accessToken ;
                kiteConnect.setAccessToken( accessToken ) ;
                kiteConnect.setPublicToken( user.publicToken ) ;

                //  Get the User Profile
                profile = kiteConnect.getProfile() ;
                logger.info( "[Profile] User Name: " + profile.userName +
                        ", Short Name: " + profile.userShortname +
                        ", User Type: " + profile.userType +
                        ", Email: " + profile.email +
                        ", Avatar: " + profile.avatarURL +
                        ", Broker: " + profile.broker +
                        ", Email: " + profile.email +
                        ", Exchanges: " + profile.exchanges +
                        ", Order Tpes: " + profile.orderTypes +
                        ", Products: " + profile.products ) ;

                //  Write the Access Token for later use.
                saveText( accessToken, accessTokenFileName ) ;
            }
            catch (IOException e) {
                logger.error( "IOException trying to get execute Kite Login.", e ) ;
                kiteConnect = null ;
            }
            catch (KiteException e) {
                logger.error( "KiteException trying to get execute Kite Login.", e ) ;
            }
        }
    }

    /**
     *  Execute the automatic web login process and retrieve the request_token.
     *
     *  @return the request_token or null if there was an error
     */
    private String autoWebLogin( String userId, String password,
                                 String twoFactorPin, String redirectUrl )  throws IOException {
        String requestToken = null ;

        //  Get the first log in URL.
        String loginUrl = kiteConnect.getLoginURL() ;
        logger.info( "Login URL: " + loginUrl ) ;

        //  Get the Kite Login page. Use the User ID & Password to submit the form.
        WebDriver webDriver = new FirefoxDriver( new FirefoxOptions().setHeadless(true) ) ;
        webDriver.get( loginUrl ) ;
        String curUrl = webDriver.getCurrentUrl() ;
        logger.info( "Login Page URL: " + curUrl ) ;
        WebElement loginField = webDriver.findElement( By.id("userid") ) ;
        WebElement pwdField = webDriver.findElement( By.id("password") ) ;
        WebElement submitButton = webDriver.findElement( By.xpath("/html/body/div[1]/div/div[2]/div[1]/div/div/div[2]/form/div[4]/button") ) ;
        loginField.sendKeys( userId ) ;
        pwdField.sendKeys( password ) ;
        logger.info( "Submitting the Login Form..." ) ;
        submitButton.click() ;
        logger.info( "Submitted the Login Form..." ) ;

        //  We are now in the 2FA page. Enter the PIN and submit the form.
        curUrl = webDriver.getCurrentUrl() ;
        logger.info( "2FA URL: " + curUrl ) ;
        WebElement twoFaField = webDriver.findElement( By.xpath("/html/body/div[1]/div/div[2]/div[1]/div/div/div[2]/form/div[2]/div/input") ) ;
        WebElement twoFaButton = webDriver.findElement( By.xpath("/html/body/div[1]/div/div[2]/div[1]/div/div/div[2]/form/div[3]/button") ) ;
        twoFaField.sendKeys( twoFactorPin ) ;
        logger.info( "Submitting the 2FA Form..." ) ;
        twoFaButton.submit() ;
        logger.info( "Submitted the 2FA Form..." ) ;
        WebDriverWait waitDriver = new WebDriverWait( webDriver, 20 ) ;
        logger.info( "Waiting for Redirect to Charanna..." ) ;
        waitDriver.until(ExpectedConditions.urlContains(redirectUrl) ) ;
        curUrl = webDriver.getCurrentUrl() ;
        logger.info( "Final URL: " + curUrl ) ;
        if ( curUrl.contains(redirectUrl) ) {
            URL url = new URL( curUrl ) ;
            Map<String, String> query_pairs = new HashMap<>();
            String query = url.getQuery();
            String[] pairs = query.split( "&" ) ;
            for ( String pair : pairs ) {
                int idx = pair.indexOf( "=" ) ;
                query_pairs.put( URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8),
                                 URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8) ) ;
            }
            requestToken = query_pairs.get( "request_token" ) ;
            logger.info( "Found Request Token - " + requestToken) ;
        }

        return requestToken ;
    }

    /**
     *  Generates a new User Session with the specified request_token.
     *
     *  @param requestToken
     *  @return
     */
    private User getUserWithSession( String requestToken, String apiSecret  ) throws KiteException, IOException {
        return kiteConnect.generateSession( requestToken, apiSecret ) ;
    }

    /**
     *  Load a file and return as String
     *
     *  @param fileName
     *  @return null if there was a problem
     */
    private String loadText( String fileName )   {
        String          returnString    = null ;

        try {
            byte[] bytes = Files.readAllBytes( new File(fileName).toPath() ) ;
            returnString = new String( bytes ) ;
        }
        catch ( IOException e ) {
            logger.error( "IOException loading text from file.", e );
        }

        return returnString ;
    }

    /**
     *  Load a file and return as String
     *
     *  @param fileName
     *  @return null if there was a problem
     */
    private void saveText( String text, String fileName )   {
        FileWriter writer = null ;

        try {
            writer = new FileWriter( fileName ) ;
            writer.write( text ) ;
        }
        catch ( IOException e ) {
            logger.error( "Opps! Problem writing to file. Check what you gave me!", e ) ;
            e.printStackTrace() ;
        }
        finally {
            if ( writer != null ) {
                try {
                    writer.close() ;
                }
                catch ( IOException e ) {
                    e.printStackTrace() ;
                }
            }
        }
    }

    /**
     *  TEST!
     *
     *  @param args
     */
    public static void main( String[] args )    {
        KiteLogin test = new KiteLogin(
                        "KITE-USER-ID", "KITE-PASSWORD", "KITE-2FA-PIN",
                              "API-KEY", "API-SECRET",
                              "APP-REDIRECT-URL") ;

        KiteConnect kiteConnect = test.getKiteConnect() ;
        List<Holding> holdings = null;
        try {
            holdings = kiteConnect.getHoldings();
        }
        catch (KiteException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        for ( Holding holding : holdings )  {
            logger.info( "[Holding] Account Id: " + holding.accountId +
                        ", Exchange: " + holding.exchange +
                        ", Instrument Token: " + holding.instrumentToken +
                        ", Trading Symbol: " + holding.tradingSymbol +
                        ", ISIN: " + holding.isin +
                        ", Product: " + holding.product +
                        ", Collateral Quantity: " + holding.collateralQuantity +
                        ", Collateral Type: " + holding.collateraltype +
                        ", Quantity: " + holding.quantity +
                        ", Price: " + holding.price +
                        ", Average Price: " + holding.averagePrice +
                        ", Last Price: " + holding.lastPrice +
                        ", Realized Quantity: " + holding.realisedQuantity +
                        ", T1 Quantity: " + holding.t1Quantity +
                        ", P/L: " + holding.pnl

            ) ;
        }

        //  Logout this time and retest.
//        test.logout();
    }

}   /*  End of the KiteLogin class. */