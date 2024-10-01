package com.example.capchon;

public class CommunicationInfo {
    private String title;
    private String contents;

    public CommunicationInfo() {}

    public CommunicationInfo(String title, String contents) {
        this.title = title;
        this.contents = contents;
    }

    public String getTitle() {
        return title;
    }
    public String getContents() {
        return contents;
    }
}
