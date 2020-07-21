package com.ironeaglex.sbs1.kafka;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("sbs1feed")
public class Sbs1FeedProperties {

    private String host;

    private int port;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    
}