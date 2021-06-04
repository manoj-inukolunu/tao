package me.tao.model;

import java.util.Map;

public class TObject {

  public Map<String, String> data;

  public int type;

  public long time;

  public long getTime() {
    return time;
  }

  public void setTime(int time) {
    this.time = time;
  }

  public Map<String, String> getData() {
    return data;
  }

  public void setData(Map<String, String> data) {
    this.data = data;
  }

  public int getType() {
    return type;
  }

  public void setType(int type) {
    this.type = type;
  }
}
