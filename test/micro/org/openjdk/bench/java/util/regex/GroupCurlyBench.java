/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it under the terms of the
 * GNU General Public License version 2 only, as published by the Free Software
 * Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy of the LICENSE file that accompanied
 * this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.org if you need additional information or have any
 * questions.
 */
package org.openjdk.bench.java.util.regex;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Benchmarks for the bookkeeping of the positions of capture groups nested
 * in a quantified group (GroupCurly).  The patterns exercise, per
 * repetition, the saving and restoring of the nested group positions that
 * happens when repetitions can be undone by backtracking.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(2)
@Warmup(iterations = 4, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 4, time = 2, timeUnit = TimeUnit.SECONDS)
@State(Scope.Thread)
public class GroupCurlyBench {

    @Param({"1000"})
    public int length;

    public String as;
    public String abPairs;
    public String dashWords;

    public Pattern directNoNested;      // (a)*, no nested captures
    public Pattern nestedGreedy;        // (?:(a))*, nested capture, no back-off
    public Pattern nestedPossessive;    // (?:a(b))*+, nested capture, possessive
    public Pattern nestedDash;          // (?:(\w+)-)+, two groups per repetition
    public Pattern nestedBackoff;       // (?:(a))*#, nested capture, full back-off

    @Setup
    public void setup() {
        as = "a".repeat(length);
        abPairs = "ab".repeat(length / 2);
        dashWords = "ab-".repeat(length / 3);

        directNoNested = Pattern.compile("(a)*");
        nestedGreedy = Pattern.compile("(?:(a))*");
        nestedPossessive = Pattern.compile("(?:a(b))*+");
        nestedDash = Pattern.compile("(?:(\\w+)-)+");
        nestedBackoff = Pattern.compile("(?:(a))*#");
    }

    @Benchmark
    public Matcher noNestedGuard() {
        return match(directNoNested, as);
    }

    @Benchmark
    public Matcher nestedGreedy() {
        return match(nestedGreedy, as);
    }

    @Benchmark
    public Matcher nestedPossessive() {
        return match(nestedPossessive, abPairs);
    }

    @Benchmark
    public Matcher nestedDash() {
        return match(nestedDash, dashWords);
    }

    /**
     * Full backtracking over all repetitions: the continuation fails at
     * every repetition count, forcing the groups to be restored for each
     * undone repetition before the whole match fails.
     */
    @Benchmark
    public Matcher nestedGreedyFullBackoff() {
        return match(nestedBackoff, as);
    }

    private static Matcher match(Pattern p, String input) {
        Matcher m = p.matcher(input);
        m.find();
        return m;
    }
}
