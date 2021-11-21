/*
 *    Copyright 2020 Valentín Bolfík
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.vb.alphapackbot;

import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.jboss.logging.Logger;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.exceptions.JedisConnectionException;

@Singleton
public class Cache {
  private static final Logger log = Logger.getLogger(Cache.class);
  private final JedisPool jedisPool;
  private boolean available;
  final Properties properties;

  /**
   * Attempts to create a Redis cache connection. Sets corresponding flags according to result.
   */
  @Inject
  public Cache(Properties properties) {
    this.properties = properties;
    JedisPoolConfig config = new JedisPoolConfig();
    config.setBlockWhenExhausted(true);
    config.setMinIdle(1);
    config.setMaxIdle(5);
    config.setMaxTotal(5);
    JedisPool jedisPool = null;
    Jedis jedis = null;
    try {
      String redisHost = System.getProperty("redis-host", "redis");
      String redisPort = System.getProperty("redis-port", "6379");
      jedisPool = new JedisPool(config, redisHost, Integer.parseInt(redisPort));
      jedis = jedisPool.getResource();
      log.info("Redis connection established.");
      this.available = true;
    } catch (JedisConnectionException jce) {
      log.warn("Unable to connect to redis, disabling cache!");
      properties.setCacheEnabled(false);
      this.available = false;
    } finally {
      if (jedis != null) {
        jedis.close();
      }
      this.jedisPool = jedisPool;
    }
  }

  /**
   * Attempts to get value specified by key and parses the rarity from it.
   *
   * @param key key of value to get from redis
   * @return {@link Optional} containing {@link RarityTypes} or empty.
   */
  public Optional<RarityTypes> getAndParse(final String key) {
    if (available && properties.isCacheEnabled()) {
      try (Jedis jedis = jedisPool.getResource()) {
        final String value = jedis.get(key);
        return RarityTypes.parse(value);
      }
    }
    return Optional.empty();
  }

  /**
   * Saves the key/value pair to database if it's available and caching is enabled.
   */
  public void save(final String key, final String value) {
    if (available && properties.isCacheEnabled()) {
      try (Jedis jedis = jedisPool.getResource()) {
        jedis.set(key, value);
      }
    }
  }
  public boolean isAvailable() {
    return this.available;
  }
}
