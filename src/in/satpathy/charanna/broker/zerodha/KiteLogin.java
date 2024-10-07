package in.satpathy.charanna.broker.zerodha;
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

public class KiteLogin {

    static {
        System.setProperty("webdriver.gecko.driver", System.getProperty("user.dir") + File.separator + "geckodriver.exe");
    }

    private static final Logger logger = LogManager.getLogger(KiteLogin.class);

    // The file where we store the Access Token.
    private final String accessTokenFileName = System.getProperty("user.dir") + File.separator + "access.token.txt";

    // The kite connect object
    private KiteConnect kiteConnect;

    // Credentials (Use your credentials here)
    private static final String API_KEY = "ivj6b19g1zxgy3hf";
    private static final String API_SECRET = "5k06hlqwavwxavoxr5bid32jfcg8xjbe";
    private static final String USER_ID = "KZ2053";
    private static final String PASSWORD = "Acz9340123";
    private static final String TWO_FACTOR_PIN = "O7KVRB73XBO7XBAZZD2BEQCFBTQNMOPR";
    private static final String REDIRECT_URL = "kite.zerodha.com";

    public KiteLogin() throws RuntimeException {
        executeKiteWebLogin(USER_ID, PASSWORD, TWO_FACTOR_PIN, API_KEY, API_SECRET, REDIRECT_URL);
    }

    public boolean logout() {
        boolean status = false;
        try {
            JSONObject resp = kiteConnect.logout();

            // Delete the access token file.
            new File(accessTokenFileName).delete();

            // We are good.
            status = true;

            logger.info(resp);
            logger.info("Logged out of Kite Connect.");
        } catch (KiteException e) {
            logger.error("KiteException while logging out.", e);
        } catch (IOException e) {
            logger.error("IOException while logging out.", e);
        }

        return status;
    }

    public KiteConnect getKiteConnect() {
        return kiteConnect;
    }

    private void executeKiteWebLogin(String userId, String password, String twoFactorPin,
                                     String apiKey, String apiSecret, String redirectUrl) {
        logger.info("Starting KiteConnect..");
        String accessToken;

        kiteConnect = new KiteConnect(apiKey);
        kiteConnect.setUserId(userId);
        kiteConnect.setSessionExpiryHook(new CharannaSessionExpiryHook());

        accessToken = loadText(accessTokenFileName);

        Profile profile = null;
        if (accessToken != null) {
            kiteConnect.setAccessToken(accessToken);
            try {
                profile = kiteConnect.getProfile();
                logger.info("We have an existing valid session.");
            } catch (IOException | KiteException e) {
                logger.error("Exception trying to get Profile to test for valid session.", e);
            }
        }

        if (profile == null) {
            try {
                String requestToken = autoWebLogin(userId, password, twoFactorPin, redirectUrl);

                User user = getUserWithSession(requestToken, apiSecret);
                accessToken = user.accessToken;
                kiteConnect.setAccessToken(accessToken);
                kiteConnect.setPublicToken(user.publicToken);

                profile = kiteConnect.getProfile();
                logger.info("Profile: " + profile.userName + ", " + profile.userShortname);

                saveText(accessToken, accessTokenFileName);
            } catch (IOException | KiteException e) {
                logger.error("Exception during Kite Login.", e);
                kiteConnect = null;
            }
        }
    }

    private String autoWebLogin(String userId, String password, String twoFactorPin, String redirectUrl) throws IOException {
        String requestToken = null;

        String loginUrl = kiteConnect.getLoginURL();
        logger.info("Login URL: " + loginUrl);

        WebDriver webDriver = new FirefoxDriver(new FirefoxOptions().setHeadless(true));
        WebDriverWait waitDriver = new WebDriverWait(webDriver, 20);

        webDriver.get(loginUrl);
        WebElement loginField = webDriver.findElement(By.id("userid"));
        WebElement pwdField = webDriver.findElement(By.id("password"));
        WebElement submitButton = webDriver.findElement(By.xpath("/html/body/div[1]/div/div[2]/div[1]/div/div/div[2]/form/div[4]/button"));
        loginField.sendKeys(userId);
        pwdField.sendKeys(password);
        submitButton.click();

        waitDriver.until(ExpectedConditions.presenceOfElementLocated(By.id("pin")));

        WebElement twoFaField = webDriver.findElement(By.cssSelector("#pin"));
        WebElement twoFaButton = webDriver.findElement(By.cssSelector(".button-orange"));
        twoFaField.sendKeys(twoFactorPin);
        twoFaButton.submit();

        waitDriver.until(ExpectedConditions.urlContains(redirectUrl));
        String curUrl = webDriver.getCurrentUrl();
        if (curUrl.contains(redirectUrl)) {
            URL url = new URL(curUrl);
            Map<String, String> query_pairs = new HashMap<>();
            String query = url.getQuery();
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                int idx = pair.indexOf("=");
                query_pairs.put(URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8),
                        URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8));
            }
            requestToken = query_pairs.get("request_token");
        }

        return requestToken;
    }

    private User getUserWithSession(String requestToken, String apiSecret) throws KiteException, IOException {
        return kiteConnect.generateSession(requestToken, apiSecret);
    }

    private String loadText(String fileName) {
        String returnString = null;
        try {
            byte[] bytes = Files.readAllBytes(new File(fileName).toPath());
            returnString = new String(bytes);
        } catch (IOException e) {
            logger.error("IOException loading text from file.", e);
        }
        return returnString;
    }

    private void saveText(String text, String fileName) {
        try (FileWriter writer = new FileWriter(fileName)) {
            writer.write(text);
        } catch (IOException e) {
            logger.error("Problem writing to file.", e);
        }
    }

    public static void main(String[] args) {
        KiteLogin test = new KiteLogin();
        KiteConnect kiteConnect = test.getKiteConnect();
        List<Holding> holdings;
        try {
            holdings = kiteConnect.getHoldings();
            for (Holding holding : holdings) {
                logger.info("[Holding] Account Id: " + holding.accountId +
                        ", Exchange: " + holding.exchange +
                        ", Instrument Token: " + holding.instrumentToken +
                        ", Trading Symbol: " + holding.tradingSymbol +
                        ", ISIN: " + holding.isin +
                        ", Product: " + holding.product +
                        ", Quantity: " + holding.quantity +
                        ", P/L: " + holding.pnl);
            }
        } catch (KiteException | IOException e) {
            e.printStackTrace();
        }
    }
}
