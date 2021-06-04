package me.tao.controller;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import me.tao.common.ShardManager;
import me.tao.model.Association;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@RestController("/assocquery")
public class AssocQueryController {

  private final JedisPool jedisPool;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final ShardManager shardManager;
  private final JdbcTemplate jdbcTemplate;

  public AssocQueryController(JedisPool jedisPool, ShardManager selector, JdbcTemplate jdbcTemplate) {
    this.jedisPool = jedisPool;
    this.shardManager = selector;
    this.jdbcTemplate = jdbcTemplate;
  }

  @RequestMapping("/get")
  public List<Association> assocGet(@RequestParam Long id1, @RequestParam Integer type, @RequestParam Set<Long> ids,
      @RequestParam Integer from, @RequestParam Integer to) {
    List<Association> ret = new ArrayList<>();
    try (Jedis jedis = jedisPool.getResource()) {
      for (Long id2 : ids) {
        if (jedis.hexists("hash", id1 + "-" + type + "-" + id2)) {
          Map<String, String> data = objectMapper.readValue(jedis.hget("hash", id1 + "-" + type + "-" + id2), Map.class);
          Association association = new Association();
          association.setId1(id1);
          association.setId2(id2);
          association.setTime(Integer.parseInt(data.get("time")));
          association.setData(data);
          ret.add(association);
        } else {
          String sql =
              "select data,time from " + getDbName(id1) + ".associations where id1=" + id1 + " AND id2=" + id2 + " AND time<" + to + " AND time>"
                  + from;
          System.out.println(sql);
          jdbcTemplate.query(sql, resultSet -> {
            try {
              Association association = new Association();
              association.setId1(id1);
              association.setId2(id2);
              association.setTime(Integer.parseInt(resultSet.getString("time")));
              Map<String, String> data = objectMapper.readValue(resultSet.getString("data"), Map.class);
              association.setData(data);
              ret.add(association);
            } catch (JsonProcessingException e) {
              e.printStackTrace();
            }
          });
        }
      }
    } catch (JsonProcessingException e) {
      e.printStackTrace();
    }
    return ret;
  }

  private String getDbName(Long id1) {
    String dbName = "db";
    StringBuilder shardId = new StringBuilder(shardManager.getSharedFromId(id1) + "");
    if (shardId.length() < 5) {
      for (int i = 0; i < 5 - shardId.length(); i++) {
        shardId.insert(0, "0");
      }
    }
    dbName += shardId;
    return dbName;
  }


  @RequestMapping("/count")
  public int assocCount(@RequestParam Integer id1, @RequestParam Integer type) {
    return 1;
  }

  //assoc range(id1, atype, pos, limit) –
  @RequestMapping("/range")
  public List<Association> assocRange(@RequestParam Integer id1, @RequestParam Integer type, @RequestParam Integer pos,
      @RequestParam Integer limit) {
    return new ArrayList<>();
  }

  //• assoc time range(id1, atype, high, low, limit)
  @RequestMapping("/timerange")
  public List<Association> assocTimeRange(@RequestParam Integer id1, @RequestParam Integer type, @RequestParam Integer low,
      @RequestParam Integer high, @RequestParam Integer limit) {
    return new ArrayList<>();
  }
}



