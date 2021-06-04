package me.tao;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TaoApplication {

  public static void main(String[] args) {
//    SpringApplication.run(TaoApplication.class, args);

    System.out.println(Long.MAX_VALUE);

    long val = Long.MAX_VALUE - 1234;

    System.out.println(Long.toBinaryString(val));
    System.out.println(Long.toBinaryString(val >> 48));

    System.out.println(241294492511762325l & 0xFFFFFFFFFl);
  }

}
