package me.tao.common;

import me.tao.model.TObject;

public interface ShardManager {

  int getSharedFromId(long id);

  String selectShard();

  long getObjectId(int shardId, long seconds, int machineId);

  long savetObject(TObject tObject);

}
