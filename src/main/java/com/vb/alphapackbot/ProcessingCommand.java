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

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jetbrains.annotations.Nullable;

/**
 * Contains available commands for processor.
 */
public enum ProcessingCommand {
  COUNT("count"),
  LAST("last"),
  FIRST("first");

  private static final Map<String, ProcessingCommand> stringValues = Stream.of(values())
      .collect(Collectors.toMap(ProcessingCommand::toString, x -> x));

  private final String command;

  ProcessingCommand(final String command) {
    this.command = command;
  }

  public static Optional<ProcessingCommand> parse(@Nullable String toParse) {
    return Optional.ofNullable(stringValues.getOrDefault(toParse, null));
  }

  @Override
  public String toString() {
    return command;
  }
}
