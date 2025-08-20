package io.github.insideranh.stellarprotect.arguments;

import lombok.Getter;

import java.util.LinkedHashSet;
import java.util.Set;

@Getter
public class UsersArg {

    private final Set<Long> userIds = new LinkedHashSet<>();

    public UsersArg(Set<Long> userIds) {
        this.userIds.addAll(userIds);
    }

    public UsersArg(long... users) {
        for (long user : users) {
            this.userIds.add(user);
        }
    }

}