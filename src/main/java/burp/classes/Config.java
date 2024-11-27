package burp.classes;

public class Config {

    public String NatsServerHost;
    public String NatsServerPort;
    public String debug;

    public String topic;


    public Config(String natsServerHost, String natsServerPort, String debug,String topic) {

        NatsServerHost = natsServerHost;
        NatsServerPort = natsServerPort;
        this.debug = debug;
        this.topic = topic;
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

    public String getTopic() {
        return topic;
    }
}
