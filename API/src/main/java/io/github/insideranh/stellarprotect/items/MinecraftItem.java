package io.github.insideranh.stellarprotect.items;

import lombok.Getter;

import java.util.Map;

@Getter
public class MinecraftItem {

    private final String cleanName;
    private final Map<String, String> properties;

    public MinecraftItem(String cleanName, Map<String, String> properties) {
        this.cleanName = cleanName;
        this.properties = properties;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Nombre: ").append(cleanName).append("\n");
        sb.append("Propiedades:\n");
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            sb.append("  ").append(entry.getKey()).append(" = ").append(entry.getValue()).append("\n");
        }
        return sb.toString();
    }

}