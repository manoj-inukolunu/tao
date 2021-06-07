package me.tao.controller;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import me.tao.common.ShardManager;
import me.tao.model.Association;
import org.checkerframework.checker.units.qual.A;
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
    return ret;
  }


  @RequestMapping("/count")
  public int assocCount(@RequestParam Long id1, @RequestParam Integer type) {
    return 1;
  }

  //assoc range(id1, atype, pos, limit) –
  @RequestMapping("/range")
  public List<Association> assocRange(@RequestParam Long id1, @RequestParam Integer type, @RequestParam Integer pos,
      @RequestParam Integer limit) throws Exception {
    return new ArrayList<>();
  }

  //• assoc time range(id1, atype, high, low, limit)
  @RequestMapping("/timerange")
  public List<Association> assocTimeRange(@RequestParam Integer id1, @RequestParam Integer type, @RequestParam Integer low,
      @RequestParam Integer high, @RequestParam Integer limit) {
    return new ArrayList<>();
  }
}



