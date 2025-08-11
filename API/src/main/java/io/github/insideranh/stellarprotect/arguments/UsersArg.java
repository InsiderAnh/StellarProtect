package io.github.insideranh.stellarprotect.arguments;

import lombok.Getter;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

@Getter
public class UsersArg {

    private final List<Long> userIds = new LinkedList<>();

    public UsersArg(Set<Long> userIds) {
        this.userIds.addAll(userIds);
    }

    public UsersArg(long... users) {
        for (long user : users) {
            this.userIds.add(user);
        }
    }

}