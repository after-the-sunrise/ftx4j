package com.after_sunrise.api.ftx4j;

import java.util.Locale;
import java.util.Objects;
import java.util.Properties;

/**
 * @author takanori.takase
 * @version 0.0.0
 */
public enum Ftx4jConfig {

    /**
     * Client API key. (required)
     */
    AUTH_APIKEY("hoge"),

    /**
     * Client API secret. (required)
     */
    AUTH_SECRET("piyo"),

    /**
     * Subaccount name, which can be omitted (null) if authenticating for main account.
     */
    AUTH_ACCOUNT(null),

    /**
     * Message signing algorithm.
     *
     * @see javax.crypto.Mac#getInstance(String)
     */
    AUTH_ALGORITHM("HmacSHA256"),

    /**
     * FIX endpoint URL.
     *
     * @see quickfix.Initiator#SETTING_SOCKET_CONNECT_PROTOCOL
     * @see quickfix.Initiator#SETTING_SOCKET_CONNECT_HOST
     * @see quickfix.Initiator#SETTING_SOCKET_CONNECT_PORT
     */
    FIX_URI("tcp://fix.ftx.com:4363"),

    /**
     * Flag to indicate if the FIX endpoint URL is secured (SSL).
     *
     * @see quickfix.mina.ssl.SSLSupport#SETTING_USE_SSL
     */
    FIX_SECURED("Y"),

    /**
     * Proxy server address. (cf: "http://192.168.1.100:3128/")
     *
     * @see quickfix.Initiator#SETTING_PROXY_TYPE
     * @see quickfix.Initiator#SETTING_PROXY_HOST
     * @see quickfix.Initiator#SETTING_PROXY_PORT
     */
    FIX_PROXY_ADDRESS(null),

    /**
     * Username for the proxy server.
     *
     * @see quickfix.Initiator#SETTING_PROXY_USER
     */
    FIX_PROXY_USER(null),

    /**
     * Password for the proxy server.
     *
     * @see quickfix.Initiator#SETTING_PROXY_PASSWORD
     */
    FIX_PROXY_PASS(null),

    /**
     * Local NIC address to bind. (cf: "tcp://192.168.1.200:32000/")
     *
     * @see quickfix.Initiator#SETTING_SOCKET_LOCAL_HOST
     * @see quickfix.Initiator#SETTING_SOCKET_LOCAL_PORT
     */
    FIX_LOCAL_ADDRESS(null),

    /**
     * Rest FIX sequence upon disconnect.
     *
     * @see quickfix.Session#SETTING_RESET_ON_DISCONNECT
     */
    FIX_RESET_ON_DISCONNECT("Y"),

    /**
     * FIX protocol version.
     *
     * @see quickfix.SessionSettings#BEGINSTRING
     */
    FIX_VERSION("FIX.4.2"),

    /**
     * Flag to specify that session never reset.
     *
     * @see quickfix.Session#SETTING_NON_STOP_SESSION
     */
    FIX_SESSION_NON_STOP("Y"),

    /**
     * Heartbeat interval in seconds.
     *
     * @see quickfix.Session#SETTING_HEARTBTINT
     */
    FIX_HEARTBEAT_SECONDS(30),


    /**
     * Reconnect interval in seconds.
     *
     * @see quickfix.Initiator#SETTING_RECONNECT_INTERVAL
     */
    FIX_RECONNECT_SECONDS(5),

    /**
     * FIX field value for "TargetCompID".
     *
     * @see quickfix.SessionSettings#TARGETCOMPID
     */
    FIX_TARGET_COMPID("FTX"),

    /**
     * Validate incoming messages.
     *
     * @see quickfix.Session#SETTING_VALIDATE_INCOMING_MESSAGE
     */
    FIX_VALIDATE_INCOMING("N"),

    /**
     * Cancel Orders on Disconnect.
     *
     * <ul>
     *     <li>"Y": all account orders will be cancelled at the end of the session.</li>
     *     <li>"S": all orders placed during the session will be cancelled at the end of the session.</li>
     *     <li>Default (null): no orders will be cancelled.</li>
     * </ul>
     */
    FIX_CANCEL_ON_DISCONNECT(null),

    ;

    private final String id;

    private final String defaultValue;

    Ftx4jConfig(Object defaultValue) {
        this.id = "ftx4j." + name().toLowerCase(Locale.US);
        this.defaultValue = Objects.toString(defaultValue, null);
    }

    public String getId() {
        return id;
    }

    public String getProperty(Properties properties) {
        return Objects.requireNonNullElseGet(properties, System::getProperties).getProperty(id, defaultValue);
    }

}
