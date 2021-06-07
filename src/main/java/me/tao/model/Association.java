package me.tao.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Joiner;
import java.util.Map;
import java.util.Objects;

public class Association {

  public long id1;
  public long id2;
  public int type;
  public long time;

  public Association(long id1, long id2, int type) {
    this.id1 = id1;
    this.id2 = id2;
    this.type = type;
  }

  @JsonIgnore
  public String getKey() {
    return Joiner.on(":").join(id1, type, id2);
  }

  @JsonIgnore
  public String getSetKey() {
    return id1 + ":" + type;
  }

  public Map<String, String> data;

  public Association(long id1, long id2, int type, long time, Map<String, String> data) {
    this.id1 = id1;
    this.id2 = id2;
    this.type = type;
    this.time = time;
    this.data = data;
  }

  public Association() {

  }

  public long getId1() {
    return id1;
  }

  public void setId1(long id1) {
    this.id1 = id1;
  }

  public long getId2() {
    return id2;
  }

  public void setId2(long id2) {
    this.id2 = id2;
  }

  public int getType() {
    return type;
  }

  public void setType(int type) {
    this.type = type;
  }

  public long getTime() {
    return time;
  }

  public void setTime(long time) {
    this.time = time;
  }

  public Map<String, String> getData() {
    return data;
  }

  public void setData(Map<String, String> data) {
    this.data = data;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Association that = (Association) o;
    return id1 == that.id1 && id2 == that.id2 && type == that.type && time == that.time && Objects.equals(data, that.data);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id1, id2, type, time, data);
  }

  @Override
  public String toString() {
    return "Association{" +
        "id1=" + id1 +
        ", id2=" + id2 +
        ", type=" + type +
        ", time=" + time +
        ", data=" + data +
        '}';
  }
}






