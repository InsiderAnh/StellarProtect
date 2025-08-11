package io.github.insideranh.stellarprotect.arguments;

import lombok.Getter;

@Getter
public class TimeArg {

    private final long start;
    private final long end;
    private String startString;
    private String endString;

    public TimeArg(String startString, String endString, long start, long end) {
        this.startString = startString;
        this.endString = endString;
        this.start = start;
        this.end = end;
    }

}