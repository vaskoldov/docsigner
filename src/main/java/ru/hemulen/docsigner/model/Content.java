package ru.hemulen.docsigner.model;

public class Content {
    private String clientId;

    public Content() {
    }

    public Content(String clientId) {
        this.clientId = clientId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }
}
