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

import com.google.common.flogger.FluentLogger;
import java.util.Optional;
import lombok.Getter;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.exceptions.JedisConnectionException;


public class Cache {
  private static final FluentLogger log = FluentLogger.forEnclosingClass();
  private static final Properties properties = Properties.getInstance();
  private final JedisPool jedisPool;
  @Getter
  private final boolean available;

  public Cache(String address) {
    JedisPoolConfig config = new JedisPoolConfig();
    config.setBlockWhenExhausted(true);
    config.setMinIdle(1);
    config.setMaxIdle(5);
    config.setMaxTotal(5);
    JedisPool jedisPool = null;
    try {
      jedisPool = new JedisPool(config, address);
      log.atInfo().log("Redis connection established.");
    } catch (JedisConnectionException jce) {
      log.atWarning().withCause(jce).log("Unable to connect to redis, disabling cache!");
      Properties.getInstance().setCacheEnabled(false);
    } finally {
      this.jedisPool = jedisPool;
      this.available = this.jedisPool != null;
    }
  }

  public Optional<RarityTypes> getAndParse(String key) {
    if (available && properties.isCacheEnabled()) {
      try (Jedis jedis = jedisPool.getResource()) {
        String value = jedis.get(key);
        return RarityTypes.parse(value);
      }
    }
    return Optional.empty();
  }

  /**
   * Saves the key/value pair to database if it's available and caching is enabled.
   */
  public void save(String key, String value) {
    if (available && properties.isCacheEnabled()) {
      try (Jedis jedis = jedisPool.getResource()) {
        jedis.set(key, value);
      }
    }
  }
}
