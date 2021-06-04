package me.tao.controller;

import java.util.Map;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController("/assocupdate")
public class AssocUpdateController {

  //• assoc add(id1, atype, id2, time, (k→v)*) –

  @RequestMapping("/add")
  public void assocGet(@RequestParam Integer id1, @RequestParam Integer type, @RequestParam Integer id2,
      @RequestParam Integer time, @RequestParam Map<String, String> data) {

  }

  //assoc delete(id1, atype, id2) –
  @RequestMapping("/delete")
  public void assocDelete(@RequestParam Integer id1, @RequestParam Integer type, @RequestParam Integer id2) {

  }

  //• assoc change type(id1, atype, id2, newtype)
  @RequestMapping("/add")
  public void assocChangeType(@RequestParam Integer id1, @RequestParam Integer type, @RequestParam Integer id2,
      @RequestParam Integer newType) {

  }
}



