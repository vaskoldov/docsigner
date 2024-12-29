package ru.hemulen.docsigner.model;

import java.util.ArrayList;

public class OssCorpSimResponse {
    String responseClientId;                        // ClientId сообщения с ответом
    ArrayList<ClaimOssResponse> claimOssResponse;   // Массив с содержимым всех RegistryRecord в ответе

    public String getResponseClientId() {
        return responseClientId;
    }

    public void setResponseClientId(String responseClientId) {
        this.responseClientId = responseClientId;
    }

    public ArrayList<ClaimOssResponse> getClaimOssResponse() {
        return claimOssResponse;
    }

    public void setClaimOssResponse(ArrayList<ClaimOssResponse> claimOssResponse) {
        this.claimOssResponse = claimOssResponse;
    }
}
