package io.github.insideranh.stellarprotect.arguments;

import lombok.Getter;

@Getter
public class PageArg {

    private final int page;
    private final int perPage;

    public PageArg(int page, int perPage) {
        this.page = page;
        this.perPage = perPage;
    }

    public int getSkip() {
        return (page - 1) * perPage;
    }

    public int getLimit() {
        return Math.min(perPage, 50);
    }

}