package io.github.insideranh.stellarprotect.config;

import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;

@Setter
@Getter
public class WorldConfigType {

    private final HashSet<String> disabledTypes = new HashSet<>();
    private boolean enabled;

}