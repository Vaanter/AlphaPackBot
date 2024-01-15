package com.vb.alphapackbot;

import com.google.common.collect.Range;
import com.google.errorprone.annotations.Immutable;

@Immutable
public record ColorRanges(Range<Integer> red, Range<Integer> green, Range<Integer> blue) {}
