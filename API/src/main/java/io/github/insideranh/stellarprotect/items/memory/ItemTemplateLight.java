package io.github.insideranh.stellarprotect.items.memory;

import io.github.insideranh.stellarprotect.utils.StringCleanerUtils;
import lombok.Getter;

@Getter
public class ItemTemplateLight {

    private final long id;
    private final String base64;
    // 0 = false | 1 = true
    private byte shorted = 0;

    public ItemTemplateLight(long id, String base64) {
        this.id = id;
        if (base64.startsWith(StringCleanerUtils.COMMON_BASE64)) {
            this.base64 = base64.substring(StringCleanerUtils.COMMON_BASE64.length());
            this.shorted = 1;
        } else {
            this.base64 = base64;
        }
    }

}