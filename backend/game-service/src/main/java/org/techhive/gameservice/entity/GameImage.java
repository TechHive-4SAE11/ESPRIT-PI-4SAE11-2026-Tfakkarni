package org.techhive.gameservice.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "game_images")
public class GameImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mini_game_id", nullable = false)
    private MiniGame miniGame;

    @Column(nullable = false)
    private String name;

    @Column(name = "image_data", nullable = false, columnDefinition = "BYTEA")
    private byte[] imageData;

    @Column(name = "image_content_type", nullable = false)
    private String imageContentType;

    @Column(name = "display_order")
    private int displayOrder;

    public GameImage() {
    }

    public GameImage(MiniGame miniGame, String name, byte[] imageData, String imageContentType, int displayOrder) {
        this.miniGame = miniGame;
        this.name = name;
        this.imageData = imageData;
        this.imageContentType = imageContentType;
        this.displayOrder = displayOrder;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public MiniGame getMiniGame() {
        return miniGame;
    }

    public void setMiniGame(MiniGame miniGame) {
        this.miniGame = miniGame;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public byte[] getImageData() {
        return imageData;
    }

    public void setImageData(byte[] imageData) {
        this.imageData = imageData;
    }

    public String getImageContentType() {
        return imageContentType;
    }

    public void setImageContentType(String imageContentType) {
        this.imageContentType = imageContentType;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }
}
