package com.qst.yunpan.pojo;

public class ShareFile extends FileCustom{
    private String shareUser;
    private String url;

    public String getShareUser() {
        return shareUser;
    }

    public void setShareUser(String shareUser) {
        this.shareUser = shareUser;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}