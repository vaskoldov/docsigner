package ru.hemulen.docsigner.model;

public class OssCorpSimRequestResponse {
    String clientId;

    public OssCorpSimRequestResponse(String clientId) {
        this.clientId = clientId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }
}
