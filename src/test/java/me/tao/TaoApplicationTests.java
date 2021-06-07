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
import me.tao.controller.AssocQueryController;
import me.tao.dao.AssociationManager;
import me.tao.model.Association;
import me.tao.model.TObject;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.io.IOUtils;
import org.checkerframework.checker.units.qual.A;
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


  AssocQueryController controller;

  private static final ObjectMapper mapper = new ObjectMapper();
  AssociationManager loader;

  @BeforeEach
  public void setUp() {
    jedisPool = new JedisPool(new JedisPoolConfig(), "localhost");
    jdbcTemplate = jdbcTemplate(mysqlDataSource());
    manager = new DbShardManager(jdbcTemplate, jedisPool);
    controller = new AssocQueryController(jedisPool, manager, jdbcTemplate);
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

  @Test
  public void testTime() {
    System.out.println(Integer.MAX_VALUE);
    System.out.println((long) Math.pow(2, 32));
  }

  @Test
  public void testObject() {

    long seconds = Instant.now().getEpochSecond();
    System.out.println("Seconds : " + seconds);
    int machineId = new Random().nextInt((int) Math.pow(2, 16));
    System.out.println("MachineId : " + machineId);
    long id = manager.getObjectId(5656, seconds, machineId);
    System.out.println(id);
    System.out.println((id >> 48) & 0xFFFF);
    System.out.println((id >> 16) & Integer.MAX_VALUE);
  }


  @Test
  void saveObjectTest() {
    for (int i = 0; i < 2; i++) {
      TObject object = new TObject();
      object.setType(new Random().nextInt(20));
      Map<String, String> data = new HashMap<>();
      object.setTime((int) (System.currentTimeMillis() / 1000));
      data.put("key-" + new Random().nextInt(), "value-" + new Random().nextInt());
      object.setData(data);
      System.out.println(manager.savetObject(object));
    }
    /*long id = manager.getObjectId(0, 0, 1);
    System.out.println(id);
    System.out.println(manager.getTypeFromObjectId(68719476736l));*/
  }

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
  }

  @Test
  public void testCreateAssociations() throws Exception {
  /*  try (Jedis jedis = jedisPool.getResource()) {
      Map<String, String> objects = jedis.hgetAll("objects");
      List<Entry<String, String>> entryList = new ArrayList<>(objects.entrySet());
      TObject one = mapper.readValue(entryList.get(new Random().nextInt(entryList.size())).getValue(), TObject.class);
      TObject two = mapper.readValue(entryList.get(new Random().nextInt(entryList.size())).getValue(), TObject.class);
      AssociationManager loader = new AssociationManager(manager, jedisPool, jdbcTemplate);
      Map<String, String> data = new HashMap<>();
      data.put("assocDataKey-" + new Random().nextInt(), "assocDataValue-" + new Random().nextInt());
      System.out.println(manager.getSharedFromId(one.getId()));
      loader.assocAdd(one.getId(), new Random().nextInt(20), two.getId(), System.currentTimeMillis() / 1000, data);
    }*/
    for (int i = 0; i < 1000; i++) {
      try (Jedis jedis = jedisPool.getResource()) {
        TObject one = mapper.readValue(jedis.hget("objects", "669314670191935"), TObject.class);
        TObject two = mapper.readValue(jedis.hget("objects", "387839693502125"), TObject.class);

        Map<String, String> data = new HashMap<>();
        data.put("assocDataKey-" + new Random().nextInt(), "assocDataValue-" + new Random().nextInt());
        loader.assocAdd(one.getId(), new Random().nextInt(20), two.getId(), System.currentTimeMillis(), data);
      }
    }
  }

  @Test
  public void updateRedis() throws Exception {
    try (Jedis jedis = jedisPool.getResource()) {
      Map<String, String> objects = jedis.hgetAll("objects");
      for (String str : objects.keySet()) {
        TObject tObject = mapper.readValue(objects.get(str), TObject.class);
        tObject.setId(Long.parseLong(str));
        jedis.hset("objects1", str, mapper.writeValueAsString(tObject));
      }
    }
  }

  @Test
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
  public void testGetShardId() throws Exception {
    System.out.println(manager.getSharedFromId(669314670191935l));
  }

  @Test
  public void testGetAssocRange() throws Exception {
    List<Association> associations = loader.assocRange(669314670191935l, 1, 0, 10);
    String dbName = getDbName(669314670191935l);
    String sql = "select * from " + dbName + ".associations where type = 1 and id1=669314670191935 order by timestamp desc limit 11";
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
    String dbName = "db";
    StringBuilder shardId = new StringBuilder(manager.getSharedFromId(id1) + "");
    int len = shardId.length();
    if (len < 5) {
      for (int i = 0; i < 5 - len; i++) {
        shardId.insert(0, "0");
      }
    }
    dbName += shardId;
    return dbName;
  }
}
