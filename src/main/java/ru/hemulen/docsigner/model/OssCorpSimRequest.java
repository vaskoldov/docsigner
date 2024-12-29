package ru.hemulen.docsigner.model;

public class OssCorpSimRequest {
    private String claimNum;
    private M2M m2m;
    private Dul dul;
    private String phoneNumber10;
    private String phoneNumber14;
    private String uki;

    public String getClaimNum() {
        return claimNum;
    }

    public void setClaimNum(String claimNum) {
        this.claimNum = claimNum;
    }

    public M2M getM2m() {
        return m2m;
    }

    public void setM2m(M2M m2m) {
        this.m2m = m2m;
    }

    public Dul getDul() {
        return dul;
    }

    public void setDul(Dul dul) {
        this.dul = dul;
    }

    public String getPhoneNumber10() {
        return phoneNumber10;
    }

    public void setPhoneNumber10(String phoneNumber10) {
        this.phoneNumber10 = phoneNumber10;
    }

    public String getPhoneNumber14() {
        return phoneNumber14;
    }

    public void setPhoneNumber14(String phoneNumber14) {
        this.phoneNumber14 = phoneNumber14;
    }

    public String getUki() {
        return uki;
    }

    public void setUki(String uki) {
        this.uki = uki;
    }
}
