package ru.hemulen.docsigner.model;

import java.util.ArrayList;

public class ClaimOssResponse {
    String recordID;
    String status;
    ArrayList<Resp> respArrayList;

    public ClaimOssResponse() {
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRecordID() {
        return recordID;
    }

    public void setRecordID(String recordID) {
        this.recordID = recordID;
    }

    public ArrayList<Resp> getRespArrayList() {
        return respArrayList;
    }

    public void setRespArrayList(ArrayList<Resp> respArrayList) {
        this.respArrayList = respArrayList;
    }
}
