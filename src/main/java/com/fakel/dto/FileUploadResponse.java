package com.fakel.dto;

public class FileUploadResponse {
    private String url;
    private String fileName;
    private String fileType;
    private long size;
    private String message;

    public FileUploadResponse() {
    }

    public FileUploadResponse(String url, String fileName, String fileType, long size, String message) {
        this.url = url;
        this.fileName = fileName;
        this.fileType = fileType;
        this.size = size;
        this.message = message;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}