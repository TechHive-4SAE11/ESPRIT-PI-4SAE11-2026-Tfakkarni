package org.techhive.gameservice.dto;

public class GameImageUpload {
    private String name;
    private String imageBase64;
    private String contentType;

    public GameImageUpload() {
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getImageBase64() { return imageBase64; }
    public void setImageBase64(String imageBase64) { this.imageBase64 = imageBase64; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
}
