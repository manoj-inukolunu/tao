package me.tao.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.Random;
import me.tao.model.TObject;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class DbShardManager implements ShardManager {


  List<String> shards = Lists.newArrayList("db00001", "db00002", "db00003", "db00004", "db00005");

  private final JdbcTemplate jdbcTemplate;
  private final JedisPool jedisPool;

  public DbShardManager(JdbcTemplate template, JedisPool jedisPool) {
    this.jdbcTemplate = template;
    this.jedisPool = jedisPool;
  }

  @Override
  public int getSharedFromId(long id) {
    return (int) ((id >> 48));
  }

  @Override
  public String selectShard() {
    return shards.get(new Random().nextInt(shards.size()));
  }

  public long getObjectId(int shardId, long time, int machineId) {
    return ((long) shardId << 48) | (time << 16) | (machineId);
  }

  @Override
  @Transactional
  public long savetObject(TObject tObject) {
    try (Jedis jedis = jedisPool.getResource()) {
      int shardId = new Random().nextInt(shards.size());
      String database = shards.get(shardId);
      long objectId = getObjectId(shardId, tObject.getTime(), new Random().nextInt((int) Math.pow(2, 16)));
      tObject.setId(objectId);
      String sql = "insert into %s.object (id,time,data,type) values (%d,%d,%s,%d)";
      jedis.hset("objects", objectId + "", new ObjectMapper().writeValueAsString(tObject));
      jdbcTemplate.update(String.format(sql, database, objectId, tObject.getTime(),
          "'" + new ObjectMapper().writeValueAsString(tObject.getData()) + "'",
          tObject.getType()));
      return objectId;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
