package org.techhive.gameservice.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "custom_game_items")
public class CustomGameItem {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "custom_game_id", nullable = false)
  private CustomGame customGame;

  @Enumerated(EnumType.STRING)
  @Column(name = "data_type", nullable = false)
  private DataPointType dataType;

  @Column(name = "data_point_id", nullable = false)
  private Long dataPointId;

  @Column(name = "display_order")
  private int displayOrder;

  public CustomGameItem() {
  }

  public CustomGameItem(CustomGame customGame, DataPointType dataType, Long dataPointId, int displayOrder) {
    this.customGame = customGame;
    this.dataType = dataType;
    this.dataPointId = dataPointId;
    this.displayOrder = displayOrder;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public CustomGame getCustomGame() {
    return customGame;
  }

  public void setCustomGame(CustomGame customGame) {
    this.customGame = customGame;
  }

  public DataPointType getDataType() {
    return dataType;
  }

  public void setDataType(DataPointType dataType) {
    this.dataType = dataType;
  }

  public Long getDataPointId() {
    return dataPointId;
  }

  public void setDataPointId(Long dataPointId) {
    this.dataPointId = dataPointId;
  }

  public int getDisplayOrder() {
    return displayOrder;
  }

  public void setDisplayOrder(int displayOrder) {
    this.displayOrder = displayOrder;
  }
}
