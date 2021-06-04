package me.tao;

import com.google.common.collect.Maps;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import javax.sql.DataSource;
import me.tao.common.DbShardManager;
import me.tao.common.ShardManager;
import me.tao.controller.AssocQueryController;
import me.tao.model.TObject;
import org.checkerframework.checker.units.qual.A;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.event.annotation.BeforeTestClass;
import org.springframework.test.context.event.annotation.BeforeTestMethod;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;


public class TaoApplicationTests {


  JedisPool jedisPool;
  ShardManager manager;
  JdbcTemplate jdbcTemplate;


  AssocQueryController controller;

  @BeforeEach
  public void setUp() {
    jedisPool = new JedisPool(new JedisPoolConfig(), "localhost");
    jdbcTemplate = jdbcTemplate(mysqlDataSource());
    manager = new DbShardManager(jdbcTemplate, jedisPool);
    controller = new AssocQueryController(jedisPool, manager, jdbcTemplate);
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
    for (int i = 0; i < 10000; i++) {
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

}
