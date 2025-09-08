package io.github.insideranh.stellarprotect.data;

import io.github.insideranh.stellarprotect.arguments.RadiusArg;
import io.github.insideranh.stellarprotect.arguments.TimeArg;
import lombok.Data;

import java.util.List;

@Data
public class RestoreSession {

    private TimeArg timeArg;
    private RadiusArg radiusArg;
    private List<Integer> actionTypes;

    public RestoreSession(TimeArg timeArg, RadiusArg radiusArg, List<Integer> actionTypes) {
        this.timeArg = timeArg;
        this.radiusArg = radiusArg;
        this.actionTypes = actionTypes;
    }

}