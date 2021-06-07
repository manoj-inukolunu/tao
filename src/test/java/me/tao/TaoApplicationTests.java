package me.tao;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.io.Resources;
import java.nio.file.Files;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import javax.sql.DataSource;
import me.tao.common.DbShardManager;
import me.tao.common.ShardManager;
import me.tao.dao.AssociationManager;
import me.tao.model.Association;
import me.tao.model.TObject;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.io.IOUtils;
import org.checkerframework.checker.units.qual.A;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;


public class TaoApplicationTests {


  JedisPool jedisPool;
  ShardManager manager;
  JdbcTemplate jdbcTemplate;


  private static final ObjectMapper mapper = new ObjectMapper();
  AssociationManager loader;

  @BeforeEach
  public void setUp() {
    jedisPool = new JedisPool(new JedisPoolConfig(), "localhost");
    jdbcTemplate = jdbcTemplate(mysqlDataSource());
    manager = new DbShardManager(jdbcTemplate, jedisPool);
    loader = new AssociationManager(manager, jedisPool, jdbcTemplate);
  }


  public JdbcTemplate jdbcTemplate(DataSource dataSource) {
    return new JdbcTemplate(dataSource);
  }


  public DataSource mysqlDataSource() {
    DriverManagerDataSource dataSource = new DriverManagerDataSource();
    dataSource.setDriverClassName("com.mysql.jdbc.Driver");
    dataSource.setUrl("jdbc:mysql://localhost:3306");
    dataSource.setUsername("root");
    dataSource.setPassword("");
    return dataSource;
  }


  private List<Long> saveObjects() {
    List<Long> ret = new ArrayList<>();
    for (int i = 0; i < 2; i++) {
      TObject object = new TObject();
      object.setType(new Random().nextInt(20));
      Map<String, String> data = new HashMap<>();
      object.setTime((int) (System.currentTimeMillis() / 1000));
      data.put("key-" + new Random().nextInt(), "value-" + new Random().nextInt());
      object.setData(data);
      ret.add(manager.savetObject(object));
    }
    return ret;
  }
/*
  @Test
  public void webhook() {
    OkHttpClient client = new OkHttpClient();
    Request request = new Request.Builder()
        .url("https://discord.com/api/webhooks/850678374199328768/6OdNCzk8rpTX6l95EsopxGM6uw1A7bwRD_08XAPsJN6Jy_sMAid6Ftn8gAyUP0d0SF5v")
        .build();

    try (Response response = client.newCall(request).execute()) {
      System.out.println(response.body().string());
    } catch (Exception e) {
      e.printStackTrace();
    }
  }*/

  @Test
  public void testCreateAssociations() throws Exception {
    dropAndCreateTables();
    deleteRedisData();
    List<Long> objects = saveObjects();
    for (int i = 0; i < 1000; i++) {
      try (Jedis jedis = jedisPool.getResource()) {
        TObject one = mapper.readValue(jedis.hget("objects", objects.get(0) + ""), TObject.class);
        TObject two = mapper.readValue(jedis.hget("objects", objects.get(1) + ""), TObject.class);
        Map<String, String> data = new HashMap<>();
        data.put("assocDataKey-" + new Random().nextInt(), "assocDataValue-" + new Random().nextInt());
        loader.assocAdd(one.getId(), new Random().nextInt(20), two.getId(), i, data);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  private void deleteRedisData() {
    try (Jedis jedis = jedisPool.getResource()) {
      jedis.select(0);
      jedis.flushDB();
    }
  }


  public void dropAndCreateTables() throws Exception {
    String associations = IOUtils.toString(Resources.getResource("associations.sql").openStream());
    String objects = IOUtils.toString(Resources.getResource("objects.sql").openStream());
    List<String> shards = Lists.newArrayList("db00001", "db00002", "db00003", "db00004", "db00005");
    jdbcTemplate.query("show databases like '%db%'", resultSet -> {
      do {
        jdbcTemplate.update("drop database " + resultSet.getString(1));
      } while (resultSet.next());
    });
    shards.forEach(shard -> {
      jdbcTemplate.update("create database " + shard + ";");
      jdbcTemplate.update(associations.replaceAll("\\$\\{dbName}", shard));
      jdbcTemplate.update(objects.replaceAll("\\$\\{dbName}", shard));
    });


  }

  @Test
  public void testGetAssocRange() throws Exception {
    List<TObject> tObjects = getObjects();
    List<Association> associations = loader.assocRange(tObjects.get(0).getId(), 1, 0, 10);
    String dbName = getDbName(tObjects.get(0).getId());
    String sql = "select * from " + dbName + ".associations where type = 1 and id1=" + tObjects.get(0).getId() + " order by timestamp desc limit 11";
    List<Association> fromDb = new ArrayList<>();
    jdbcTemplate.query(sql, resultSet -> {
      do {
        //long id1, long id2, int type, long time, Map<String, String> data
        try {
          Association association = new Association(resultSet.getLong("id1"), resultSet.getLong("id2"), resultSet.getInt("type"), resultSet.getLong(
              "timestamp"), mapper.readValue(resultSet.getString("data"), Map.class));
          fromDb.add(association);
        } catch (JsonProcessingException e) {
          e.printStackTrace();
        }
      } while (resultSet.next());
    });
    assertEquals(fromDb.size(), associations.size());
    for (int i = 0; i < associations.size(); i++) {
      assertEquals(associations.get(i).toString(), fromDb.get(i).toString());
    }
  }

  private List<TObject> getObjects() throws JsonProcessingException {
    Jedis jedis = jedisPool.getResource();
    Map<String, String> objects = jedis.hgetAll("objects");
    List<TObject> tObjects = new ArrayList<>();
    for (String str : objects.keySet()) {
      TObject tObject = mapper.readValue(objects.get(str), TObject.class);
      tObjects.add(tObject);
    }
    return tObjects;
  }


  @Test
  public void testGetAssocRangeInBetween() throws Exception {
    List<Association> associations = loader.assocRange(669314670191935l, 1, 13, 16);
    String dbName = getDbName(669314670191935l);
    String sql = "select * from " + dbName + ".associations where type = 1 and id1=669314670191935 order by timestamp desc";
    List<Association> fromDb = new ArrayList<>();
    jdbcTemplate.query(sql, resultSet -> {
      do {
        //long id1, long id2, int type, long time, Map<String, String> data
        try {
          Association association = new Association(resultSet.getLong("id1"), resultSet.getLong("id2"), resultSet.getInt("type"), resultSet.getLong(
              "timestamp"), mapper.readValue(resultSet.getString("data"), Map.class));
          fromDb.add(association);
        } catch (JsonProcessingException e) {
          e.printStackTrace();
        }
      } while (resultSet.next());
    });
    int pos = 13;
    for (int i = 0; i < associations.size(); i++) {
      assertEquals(associations.get(i).toString(), fromDb.get(pos++).toString());
    }
  }

  private String getDbName(Long id1) {
    List<String> shards = Lists.newArrayList("db00001", "db00002", "db00003", "db00004", "db00005");
    return shards.get(manager.getSharedFromId(id1));
  }

  @Test
  public void testGetAssocBetween() throws Exception {
    List<TObject> objects = getObjects();
    TObject tObject = objects.get(0);
    List<Association> list = loader.assocTimeRange(tObject.getId(), 1, 10, 15, 3);
    list.forEach(System.out::println);
  }
}
