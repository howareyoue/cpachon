package com.example.capchon;

public class CommunicationInfo {
    private String id; // Firebase에서 자동 생성된 ID
    private String title;
    private String contents;

    // 기본 생성자
    public CommunicationInfo() {
        // Firebase에서 필요로 하는 기본 생성자
    }

    // 두 개의 매개변수를 받는 생성자
    public CommunicationInfo(String title, String contents) {
        this.id = null; // ID는 null로 설정
        this.title = title;
        this.contents = contents;
    }

    // 세 개의 매개변수를 받는 생성자
    public CommunicationInfo(String id, String title, String contents) {
        this.id = id;
        this.title = title;
        this.contents = contents;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id; // ID를 설정하는 메서드
    }

    public String getTitle() {
        return title;
    }

    public String getContents() {
        return contents;
    }
}
