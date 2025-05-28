package io.ebean.datasource;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;


public class DataSourceConfigTest {

  @Test
  public void addProperty() {

    DataSourceConfig config = new DataSourceConfig();
    assertNull(config.getCustomProperties());

    config.addProperty("useSSL", false);
    final Map<String, String> extra = config.getCustomProperties();
    assertThat(extra).hasSize(1);
    assertThat(extra.get("useSSL")).isEqualTo("false");

    config.addProperty("foo", 42);
    assertThat(extra.get("foo")).isEqualTo("42");

    config.addProperty("bar", "silly");
    assertThat(extra.get("bar")).isEqualTo("silly");
    assertThat(extra).hasSize(3);
  }

  @Test
  public void parseCustom() {

    DataSourceConfig config = new DataSourceConfig();
    Map<String, String> map = new HashMap<>();
    config.parseCustom("a=1;b=2;c=3", map);

    assertThat(map).hasSize(3);
    assertThat(map.get("a")).isEqualTo("1");
    assertThat(map.get("b")).isEqualTo("2");
    assertThat(map.get("c")).isEqualTo("3");

    config.parseCustom("a=4;b=5;d=6", map);
    assertThat(map).hasSize(4);
    assertThat(map.get("a")).isEqualTo("4");
    assertThat(map.get("b")).isEqualTo("5");
    assertThat(map.get("c")).isEqualTo("3");
    assertThat(map.get("d")).isEqualTo("6");
  }

  @Test
  public void parseSql() {

    DataSourceConfig config = new DataSourceConfig();
    List<String> list = new ArrayList<>();
    config.parseSql("SET NAMES utf8mb4;SET collation_connection = 'utf8mb4_bin';SET wait_timeout = 28800", list);
    assertThat(list).containsExactly("SET NAMES utf8mb4", "SET collation_connection = 'utf8mb4_bin'", "SET wait_timeout = 28800");

    config.parseSql("delimiter $$CREATE PROCEDURE test1\n" +
      "BEGIN\n" +
      "DECLARE xy INT DEFAULT FALSE;\n" +
      "END$$CREATE PROCEDURE test2\n" +
      "BEGIN\n" +
      "DECLARE ab INT DEFAULT FALSE;\n" +
      "END$$", list);
    assertThat(list).containsExactly(
      "SET NAMES utf8mb4",
      "SET collation_connection = 'utf8mb4_bin'",
      "SET wait_timeout = 28800",
      "CREATE PROCEDURE test1\n" +
        "BEGIN\n" +
        "DECLARE xy INT DEFAULT FALSE;\n" +
        "END",
      "CREATE PROCEDURE test2\n" +
        "BEGIN\n" +
        "DECLARE ab INT DEFAULT FALSE;\n" +
        "END");

  }

  @Test
  public void isEmpty() {
    DataSourceConfig config = new DataSourceConfig();
    assertThat(config.isEmpty()).isTrue();

    config.setUrl("foo");
    assertThat(config.isEmpty()).isFalse();

    config = new DataSourceConfig();
    config.setUsername("foo");
    assertThat(config.isEmpty()).isFalse();

    config = new DataSourceConfig();
    config.setPassword("foo");
    assertThat(config.isEmpty()).isFalse();

    config = new DataSourceConfig();
    config.setDriver("foo");
    assertThat(config.isEmpty()).isFalse();
  }

  @Test
  void initial_expect_inRangeMinMax() {
    DataSourceConfig readOnly = new DataSourceConfig();
    readOnly.setMinConnections(10);
    readOnly.setMaxConnections(30);

    readOnly.initialConnections(1);
    assertThat(readOnly.getInitialConnections()).isEqualTo(10);

    readOnly.initialConnections(100);
    assertThat(readOnly.getInitialConnections()).isEqualTo(30);
  }

  @Test
  public void copy() {

    DataSourceConfig source = new DataSourceConfig();
    source.setMinConnections(42);
    source.setMaxConnections(45);
    source.setUsername("un");
    source.setPassword("pw");
    source.setUrl("url");
    source.setReadOnlyUrl("readOnlyUrl");
    source.setSchema("sch");
    source.catalog("cat");

    Map<String, String> customSource = new LinkedHashMap<>();
    customSource.put("a", "a");
    customSource.put("b", "b");
    source.setCustomProperties(customSource);


    DataSourceConfig copy = source.copy();
    assertEquals("un", copy.getUsername());
    assertEquals("pw", copy.getPassword());
    assertEquals("url", copy.getUrl());
    assertEquals("readOnlyUrl", copy.getReadOnlyUrl());
    assertEquals("sch", copy.getSchema());
    assertEquals("cat", copy.catalog());
    assertEquals(42, copy.getMinConnections());
    assertEquals(42, copy.getInitialConnections());
    assertEquals(45, copy.getMaxConnections());

    customSource.put("a", "modifiedA");
    customSource.put("c", "newC");

    assertEquals("a", copy.getCustomProperties().get("a"));
    assertEquals("b", copy.getCustomProperties().get("b"));
    assertNull(copy.getCustomProperties().get("c"));
  }

  @Test
  public void defaults() {

    DataSourceConfig config = create();
    config.initialConnections(6);

    var readOnly = new DataSourceConfig().setDefaults(config);

    assertThat(readOnly.getDriver()).isEqualTo(config.getDriver());
    assertThat(readOnly.getUrl()).isEqualTo(config.getUrl());
    assertThat(readOnly.getUsername()).isEqualTo(config.getUsername());
    assertThat(readOnly.getPassword()).isEqualTo(config.getPassword());
    assertThat(readOnly.getSchema()).isEqualTo(config.getSchema());
    assertThat(readOnly.catalog()).isEqualTo(config.catalog());
    assertThat(readOnly.getMinConnections()).isEqualTo(config.getMinConnections());
    assertThat(readOnly.getInitialConnections()).isEqualTo(config.getInitialConnections());
    assertThat(readOnly.getCustomProperties()).containsKeys("useSSL");
  }

  @Test
  void setDefaults_expect_connectionsDefault() {
    DataSourceConfig readOnly = new DataSourceConfig();
    readOnly.setDefaults(create());
    assertThat(readOnly.getMinConnections()).isEqualTo(1);
    assertThat(readOnly.getInitialConnections()).isEqualTo(1);
    assertThat(readOnly.getMaxConnections()).isEqualTo(20);
  }

  @Test
  void setDefaults_when_explicit() {
    DataSourceConfig readOnly = new DataSourceConfig();
    readOnly.setMinConnections(21);
    readOnly.initialConnections(25);
    readOnly.setMaxConnections(32);
    readOnly.setDefaults(create());
    assertThat(readOnly.getMinConnections()).isEqualTo(21);
    assertThat(readOnly.getInitialConnections()).isEqualTo(25);
    assertThat(readOnly.getMaxConnections()).isEqualTo(32);
  }

  @Test
  void setDefaults_when_explicitSameAsNormalDefaults() {
    DataSourceConfig readOnly = new DataSourceConfig();
    readOnly.setMinConnections(2);
    readOnly.setMaxConnections(200);

    // act
    readOnly.setDefaults(create());

    assertThat(readOnly.getMinConnections()).isEqualTo(2);
    assertThat(readOnly.getInitialConnections()).isEqualTo(2);
    assertThat(readOnly.getMaxConnections()).isEqualTo(200);
  }

  @Test
  public void defaults_someOverride() {
    DataSourceConfig readOnly = new DataSourceConfig();
    readOnly.setUsername("foo2");
    readOnly.setUrl("jdbc:postgresql://127.0.0.2:5432/unit");
    readOnly.validateOnHeartbeat(false);
    readOnly.setMinConnections(3);

    DataSourceBuilder configBuilder = create();
    DataSourceConfig readOnly2 = readOnly.setDefaults(configBuilder);

    var config = configBuilder.settings();
    assertThat(readOnly2).isSameAs(readOnly);
    assertThat(readOnly.getPassword()).isEqualTo(config.getPassword());
    assertThat(readOnly.getDriver()).isEqualTo(config.getDriver());
    assertThat(readOnly.getUrl()).isEqualTo("jdbc:postgresql://127.0.0.2:5432/unit");
    assertThat(readOnly.getUsername()).isEqualTo("foo2");
    assertThat(readOnly.getMinConnections()).isEqualTo(3);
    assertThat(readOnly.getInitialConnections()).isEqualTo(3);
    assertThat(readOnly.getMaxConnections()).isEqualTo(20);
    assertThat(readOnly.isShutdownOnJvmExit()).isFalse();
    assertThat(readOnly.isValidateOnHeartbeat()).isFalse();
  }

  @Test
  public void defaults_someOverride2() {
    DataSourceConfig readOnly = new DataSourceConfig();
    readOnly.setUrl("jdbc:postgresql://127.0.0.2:5432/unit");

    DataSourceBuilder configBuilder = create().shutdownOnJvmExit(true).validateOnHeartbeat(false);
    DataSourceConfig readOnly2 = readOnly.setDefaults(configBuilder);

    assertThat(readOnly2).isSameAs(readOnly);
    assertThat(readOnly.isShutdownOnJvmExit()).isTrue();
    assertThat(readOnly.isValidateOnHeartbeat()).isFalse();
  }

  private DataSourceConfig create() {
    return new DataSourceConfig()
      .setDriver("org.postgresql.Driver")
      .setUrl("jdbc:postgresql://127.0.0.1:5432/unit")
      .setUsername("foo")
      .setPassword("bar")
      .setMinConnections(1)
      .setMaxConnections(20)
      .addProperty("useSSL", false);
  }

  @Test
  public void loadSettings() throws IOException {
    Properties props = new Properties();
    props.load(getClass().getResourceAsStream("/example.properties"));

    var config = new DataSourceConfig().loadSettings(props, "foo");
    assertConfigValues(config);
    assertThat(config.isShutdownOnJvmExit()).isTrue();
    assertThat(config.isValidateOnHeartbeat()).isTrue();
    assertThat(config.isValidateOnHeartbeat()).isTrue();
  }

  @Test
  public void load_prefix() throws IOException {
    Properties props = new Properties();
    props.load(getClass().getResourceAsStream("/example2.properties"));

    var config = new DataSourceConfig().load(props, "bar");
    assertConfigValues(config);
    assertThat(config.isShutdownOnJvmExit()).isFalse();
    assertThat(config.isValidateOnHeartbeat()).isTrue();
  }

  @Test
  public void from_prefix() throws IOException {
    Properties props = new Properties();
    props.load(getClass().getResourceAsStream("/example2.properties"));

    var builder = DataSourceBuilder.from(props, "bar");
    assertConfigValues(builder.settings());
    assertThat(builder.settings().getInitialConnections()).isEqualTo(12);
  }

  @Test
  public void apply() {
    var builder = DataSourceBuilder.create()
      .apply(this::myConfig)
      .minConnections(3);
    assertThat(builder.settings().getMaxConnections()).isEqualTo(100);
    assertThat(builder.settings().getMinConnections()).isEqualTo(3);
  }

  @Test
  public void alsoIf() {
    var builder = DataSourceBuilder.create()
      .alsoIf(() -> true, this::myConfig)
      .minConnections(3);

    assertThat(builder.settings().getMaxConnections()).isEqualTo(100);
    assertThat(builder.settings().getMinConnections()).isEqualTo(3);
    assertThat(builder.settings().getInitialConnections()).isEqualTo(3);
  }

  @Test
  public void alsoIf_notApplied() {
    var builder = DataSourceBuilder.create()
      .alsoIf(() -> false, this::myConfig)
      .minConnections(3)
      .initialConnections(6);

    assertThat(builder.settings().getMaxConnections()).isEqualTo(200);
    assertThat(builder.settings().getMinConnections()).isEqualTo(3);
    assertThat(builder.settings().getInitialConnections()).isEqualTo(6);
  }

  private void myConfig(DataSourceBuilder.Settings builder) {
    builder.maxConnections(100);
  }

  @Test
  public void load_noPrefix() throws IOException {
    Properties props = new Properties();
    props.load(getClass().getResourceAsStream("/example3.properties"));

    var config = new DataSourceConfig().load(props);
    assertConfigValues(config);

    var config2 = new DataSourceConfig().load(props, null);
    assertConfigValues(config2);
  }

  @Test
  public void from_noPrefix() throws IOException {
    Properties props = new Properties();
    props.load(getClass().getResourceAsStream("/example3.properties"));

    var builder = DataSourceBuilder.from(props);
    assertConfigValues(builder.settings());

    var builder2 = DataSourceBuilder.from(props, null);
    assertConfigValues(builder2.settings());
  }

  private static void assertConfigValues(DataSourceBuilder.Settings config) {
    assertThat(config.getReadOnlyUrl()).isEqualTo("myReadOnlyUrl");
    assertThat(config.getUrl()).isEqualTo("myUrl");
    assertThat(config.getUsername()).isEqualTo("myusername");
    assertThat(config.getPassword()).isEqualTo("mypassword");
    assertThat(config.getSchema()).isEqualTo("myschema");
    assertThat(config.catalog()).isEqualTo("mycat");
    assertThat(config.getApplicationName()).isEqualTo("myApp");
    Properties clientInfo = config.getClientInfo();
    assertThat(clientInfo.getProperty("ClientUser")).isEqualTo("ciu");
    assertThat(clientInfo.getProperty("ClientHostname")).isEqualTo("cih");
  }
}
