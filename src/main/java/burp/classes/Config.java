package burp.classes;

public class Config {

    public String NatsServerHost;
    public String NatsServerPort;
    public String debug;


    public Config(String natsServerHost, String natsServerPort, String debug) {

        NatsServerHost = natsServerHost;
        NatsServerPort = natsServerPort;
        this.debug = debug;
    }

    public String getNatsServerHost() {
        return NatsServerHost;
    }

    public String getNatsServerPort() {
        return NatsServerPort;
    }

    public String getDebug() {
        return debug;
    }
}
