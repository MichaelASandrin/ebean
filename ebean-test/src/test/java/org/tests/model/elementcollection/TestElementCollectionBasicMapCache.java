package org.tests.model.elementcollection;

import io.ebean.xtest.BaseTestCase;
import io.ebean.DB;
import io.ebean.annotation.PersistBatch;
import io.ebean.test.LoggedSql;
import io.ebeaninternal.api.SpiEbeanServer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class TestElementCollectionBasicMapCache extends BaseTestCase {

  @Test
  public void test() {

    EcmPerson person = new EcmPerson("CacheMap");
    person.getPhoneNumbers().put("home", "021 1234");
    person.getPhoneNumbers().put("work", "021 4321");
    DB.save(person);


    EcmPerson one = DB.find(EcmPerson.class)
      .setId(person.getId())
      .fetch("phoneNumbers")
      .findOne();

    one.getPhoneNumbers().size();

    LoggedSql.start();

    EcmPerson two = DB.find(EcmPerson.class)
      .setId(person.getId())
      .findOne();

    two.setName("CacheMod");
    two.getPhoneNumbers().put("mob", "027 234234");

    List<String> sql = LoggedSql.collect();
    assertThat(sql).isEmpty(); // cache hit containing phone numbers

    DB.save(two);

    sql = LoggedSql.collect();
    if (isPersistBatchOnCascade()) {
      assertThat(sql).hasSize(7);
      assertSqlBind(sql, 4, 6);
    } else {
      assertThat(sql).hasSize(5);
    }

    DB.save(two);

    sql = LoggedSql.collect();
    assertThat(sql).isEmpty(); // no change

    EcmPerson three = DB.find(EcmPerson.class)
      .setId(person.getId())
      .findOne();

    assertThat(three.getName()).isEqualTo("CacheMod");
    assertThat(three.getPhoneNumbers().toString()).contains("021 1234", "021 4321", "027 234234");

    sql = LoggedSql.collect();
    assertThat(sql).isEmpty(); // cache hit

    three.getPhoneNumbers().put("oth", "09 6534");
    three.getPhoneNumbers().remove("home");
    three.getPhoneNumbers().remove("work");
    DB.save(three);

    sql = LoggedSql.collect();
    if (isPersistBatchOnCascade()) {
      assertThat(sql).hasSize(5); // cache hit
      assertSqlBind(sql, 3, 4);
    } else {
      assertThat(sql).hasSize(3); // cache hit
    }

    EcmPerson four = DB.find(EcmPerson.class)
      .setId(person.getId())
      .fetch("phoneNumbers")
      .findOne();

    assertThat(four.getName()).isEqualTo("CacheMod");
    assertThat(four.getPhoneNumbers().toString()).contains("027 234234", "09 6534");
    assertThat(four.getPhoneNumbers()).hasSize(2);

    sql = LoggedSql.collect();
    assertThat(sql).isEmpty(); // cache hit

    DB.delete(four);

    LoggedSql.stop();
  }

  @Override
  public boolean isPersistBatchOnCascade() {
    return ((SpiEbeanServer) DB.getDefault()).databasePlatform().getPersistBatchOnCascade() != PersistBatch.NONE;
  }
}
