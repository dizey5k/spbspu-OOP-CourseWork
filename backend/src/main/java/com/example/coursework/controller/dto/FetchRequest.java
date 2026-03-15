package com.example.coursework.controller.dto;

import java.util.List;

public class FetchRequest {
    private List<String> sources;
    private String format;
    private String filename;
    private boolean append;

    public List<String> getSources() {return sources;}
    public void setSources(List<String> sources) {this.sources = sources;}
    public String getFormat() {return format;}
    public void setFormat(String format) {this.format = format;}
    public String getFilename() {return filename;}
    public void setFilename(String filename) {this.filename = filename;}
    public boolean isAppend() {return append;}
    public void setAppend(boolean append) {this.append = append;}
}