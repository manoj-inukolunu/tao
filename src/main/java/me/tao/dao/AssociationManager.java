package me.tao.dao;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import me.tao.common.ShardManager;
import me.tao.model.Association;
import org.jetbrains.annotations.NotNull;
import org.springframework.jdbc.core.JdbcTemplate;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class AssociationManager {

  private final ShardManager shardManager;
  private final JedisPool jedisPool;
  private final JdbcTemplate jdbcTemplate;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public AssociationManager(ShardManager manager, JedisPool jedisPool, JdbcTemplate jdbcTemplate) {
    this.shardManager = manager;
    this.jedisPool = jedisPool;
    this.jdbcTemplate = jdbcTemplate;
  }

  private List<Association> getFromDbWithPosAndLimit(long id1, int type, int pos, int limit) {
    String sql =
        "select data,timestamp,id2 from " + getDbName(id1) + ".associations where id1=" + id1 + " AND type=" + type + " OFFSET " + pos +
            "LIMIT " + limit;
    return executeQueryAndGetResponse(id1, type, sql);
  }

  private void addToRedis(Association association) throws Exception {
    Jedis jedis = jedisPool.getResource();
    String key = association.getId1() + ":" + association.getType();
    jedis.zadd(key, association.getTime(), objectMapper.writeValueAsString(association));
    jedis.hset("associations", key, objectMapper.writeValueAsString(association));
  }

  private int getCount(Long id1, Integer type) {
    String sql = "select count(*) from %s where id1=%d and type=%d";
    try (Jedis jedis = jedisPool.getResource()) {
      String key = id1 + ":" + type;
      if (jedis.hexists("counts", key)) {
        return Integer.parseInt(jedis.hget("counts", key));
      } else {
        int count = jdbcTemplate.query(String.format(sql, getTableName(id1, "associations"), id1, type), (resultSet) -> {
          resultSet.next();
          return resultSet.getInt(1);
        });
        jedis.hset("counts", key, count + "");
        return count;
      }
    }
  }


  private String getDbName(Long id1) {
    String dbName = "db";
    StringBuilder shardId = new StringBuilder(shardManager.getSharedFromId(id1) + "");
    int len = shardId.length();
    if (len < 5) {
      for (int i = 0; i < 5 - len; i++) {
        shardId.insert(0, "0");
      }
    }
    dbName += shardId;
    return dbName;
  }

  public List<Association> assocRange(Long id1, Integer type, Integer pos, Integer limit) throws Exception {
    int count = getCount(id1, type);
    String key = id1 + ":" + type;
    if (pos + limit <= count) {
      Jedis jedis = jedisPool.getResource();
      Set<String> data = jedis.zrevrange(key, pos, pos + limit);
      List<Association> list = new ArrayList<>();
      for (String str : data) {
        list.add(objectMapper.readValue(str, Association.class));
      }
      return list;
    } else {
      int dbLimit = (pos + limit) - count;
      return getFromDbWithPosAndLimit(id1, type, count, dbLimit);
    }
  }

  public List<Association> assocTimeRange(Long id1, Integer type, Integer low, Integer high, Integer limit) throws Exception {
    String key = id1 + ":" + type;
    Jedis jedis = jedisPool.getResource();
    List<Association> list = new ArrayList<>();
    if (jedis.exists(key)) {
      Association last = objectMapper.readValue(jedis.zrange(key, 0, 0).iterator().next(), Association.class);
      //high > low>=last
      if (low >= last.getTime()) {
        Set<String> data = jedis.zrevrangeByScore(key, high, low, 0, limit);
        for (String str : data) {
          list.add(objectMapper.readValue(str, Association.class));
        }
      } else if (high >= last.getTime() && low < last.getTime()) {
        String sql =
            "select data,timestamp,id2 from " + getDbName(id1) + ".associations where id1=" + id1 + " AND type=" + type + " AND timestamp>=" + low +
                " AND timestamp<" + last.getTime();
        executeQueryAndGetResponse(id1, type, sql);
        return assocTimeRange(id1, type, low, high, limit);
      } else {
        list = getAssociationsUpto(id1, type, low);
        return list.subList(list.size() - limit - 1, list.size() - 1);
      }
    } else {
      getAssociationsUpto(id1, type, low);
      return assocTimeRange(id1, type, low, high, limit);
    }
    throw new RuntimeException("Should not come here ");
  }

  public List<Association> getAssociationsUpto(Long id1, Integer type, Integer time) {
    String sql =
        "select data,timestamp,id2 from " + getDbName(id1) + ".associations where id1=" + id1 + " AND type=" + type + " AND timestamp>=" + time;
    return executeQueryAndGetResponse(id1, type, sql);
  }

  @NotNull
  private List<Association> executeQueryAndGetResponse(Long id1, Integer type, String sql) {
    List<Association> list = new ArrayList<>();
    jdbcTemplate.query(sql, resultSet -> {
      while (resultSet.next()) {
        Association association = new Association();
        association.setTime(resultSet.getLong("timestamp"));
        association.setId1(id1);
        association.setTime(type);
        association.setId2(resultSet.getLong("id2"));
        try {
          association.setData(objectMapper.readValue(resultSet.getString("data"), Map.class));
          addToRedis(association);
        } catch (Exception e) {
          e.printStackTrace();
        }
        list.add(association);
      }
    });
    return list;
  }

  private void addAssociationToRedis(Association association) throws Exception {
    try (Jedis jedis = jedisPool.getResource()) {
      jedis.hset("associations", association.getKey(), objectMapper.writeValueAsString(association));
      jedis.zadd(association.getSetKey(), association.getTime(), objectMapper.writeValueAsString(association));
    }
  }


  public void assocAdd(long id1, int type, long id2, long time, Map<String, String> data) throws Exception {
    Association association = new Association(id1, id2, type, time, data);
    addAssociationToRedis(association);
    addAssociationToDB(association);
    association = new Association(id2, id1, type, time, data);
    addAssociationToRedis(association);
    addAssociationToDB(association);
  }

  private void addAssociationToDB(Association association) throws Exception {
    jdbcTemplate
        .update(String
            .format("insert into %s (id1,id2,timestamp,data,type) values(%d,%d,%d,%s,%d)", getTableName(association.getId1(), "associations"),
                association.getId1(),
                association.getId2(),
                association.getTime(), "'" + objectMapper.writeValueAsString(association.getData()) + "'", association.getType()));
  }

  private void deleteFromRedis(Association association) throws Exception {
    try (Jedis jedis = jedisPool.getResource()) {
      jedis.hdel("associations", association.getKey());
      jedis.zrem(association.getSetKey(), objectMapper.writeValueAsString(association));
    }
  }

  private void deleteFromDb(Association association) throws Exception {
    jdbcTemplate
        .update(
            String.format("delete from %s where id1=%d and id2=%d and type=%d", getTableName(association.getId1(), "associations"),
                association.getId1(),
                association.getId2(),
                association.getType()));
  }

  public void assocDelete(long id1, int type, long id2) throws Exception {
    deleteFromRedis(new Association(id1, id2, type));
    deleteFromRedis(new Association(id2, id2, type));
    deleteFromDb(new Association(id1, id2, type));
    deleteFromDb(new Association(id2, id1, type));
  }

  public void assocChangeType(long id1, int type, long id2, int newType) throws Exception {
    deleteFromRedis(new Association(id1, id2, type));
    deleteFromRedis(new Association(id2, id2, type));
    jdbcTemplate.update("update %s set type=%d where id1=%d and id2=%d and type=%d", getTableName(id1, "associations"), newType, id1, id2, type);
  }

  private String getTableName(long id, String tableName) {
    return getDbName(id) + "." + tableName;
  }
}
