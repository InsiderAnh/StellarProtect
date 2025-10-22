package io.github.insideranh.stellarprotect.arguments;

import lombok.Data;

@Data
public class HashTagsArg {

    private boolean preview;
    private boolean verbose;
    private boolean silent;
    private boolean count;
    private boolean session;
    private boolean entities;

    public HashTagsArg(String[] arguments) {
        for (String arg : arguments) {
            if (arg.equals("#preview")) {
                this.preview = true;
            }
            if (arg.equals("#verbose")) {
                this.verbose = true;
            }
            if (arg.equals("#silent")) {
                this.silent = true;
            }
            if (arg.equals("#count")) {
                this.count = true;
            }
            if (arg.equals("#session")) {
                this.session = true;
            }
            if (arg.equals("#entities")) {
                this.entities = true;
            }
        }
    }

}