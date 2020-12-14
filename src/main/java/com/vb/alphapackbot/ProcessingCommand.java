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
