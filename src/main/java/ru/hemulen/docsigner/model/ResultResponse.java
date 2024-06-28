package ru.hemulen.docsigner.model;

import java.util.ArrayList;

public class ResultResponse {
    private String result;

    private ArrayList<String> attachments = new ArrayList<>();
    private Error error;

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public Error getError() {
        return error;
    }

    public void setError(Error error) {
        this.error = error;
    }

    public ArrayList<String> getAttachments() {
        return attachments;
    }

    public void setAttachments(ArrayList<String> attachments) {
        this.attachments = attachments;
    }

    public void addAttachment(String attachment) {
        this.attachments.add(attachment);
    }

}
