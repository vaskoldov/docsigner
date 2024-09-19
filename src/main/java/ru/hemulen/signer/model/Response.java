package ru.hemulen.signer.model;

public class Response {
    private String signPath;
    private Error error;

    public Response() {
    }

    public String getSignPath() {
        return signPath;
    }

    public void setSignPath(String signPath) {
        this.signPath = signPath;
    }

    public Error getError() {
        return error;
    }

    public void setError(Error error) {
        this.error = error;
    }
}
