package com.example.coursework.controller.dto;

public class ApiInfoDto {
    private String name;
    private String displayName;

    public ApiInfoDto(String name, String displayName) {
        this.name = name;
        this.displayName = displayName;
    }

    public String getDisplayName() {return displayName;}
    public void setDisplayName(String displayName) {this.displayName = displayName;}
    public String getName() {return name;}
    public void setName(String name) {this.name = name;}
}