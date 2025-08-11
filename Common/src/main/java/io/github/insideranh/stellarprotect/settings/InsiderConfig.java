package io.github.insideranh.stellarprotect.settings;

import io.github.insideranh.stellarprotect.StellarProtect;
import lombok.Getter;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class InsiderConfig {

    @Getter
    private final YamlConfiguration config;
    private final File file;
    private final JavaPlugin javaPlugin;
    private final boolean comments;

    public InsiderConfig(JavaPlugin javaPlugin, String s, boolean defaults, boolean comments) {
        this(javaPlugin, s, defaults, comments, false);
    }

    public InsiderConfig(JavaPlugin javaPlugin, String s, boolean defaults, boolean comments, boolean reset) {
        this.javaPlugin = javaPlugin;
        this.comments = comments;
        this.file = new File(javaPlugin.getDataFolder(), s + ".yml");
        if (reset) {
            file.delete();
        }
        this.config = YamlConfiguration.loadConfiguration(this.file);
        try {
            if (!this.file.exists()) {
                file.createNewFile();

                Reader reader;
                if (comments) {
                    reader = new InputStreamReader(getConfigContent(new InputStreamReader(javaPlugin.getResource(s + ".yml"), StandardCharsets.UTF_8)));
                } else {
                    reader = new InputStreamReader(javaPlugin.getResource(s + ".yml"), StandardCharsets.UTF_8);
                }

                YamlConfiguration loadConfiguration = YamlConfiguration.loadConfiguration(reader);
                this.config.addDefaults(loadConfiguration);
                this.config.options().copyDefaults(true);

                if (comments) {
                    save();
                } else {
                    this.config.save(file);
                }
            } else {
                if (defaults) {
                    Reader reader;
                    if (comments) {
                        reader = new InputStreamReader(getConfigContent(new InputStreamReader(javaPlugin.getResource(s + ".yml"), StandardCharsets.UTF_8)));
                    } else {
                        reader = new InputStreamReader(javaPlugin.getResource(s + ".yml"), StandardCharsets.UTF_8);
                    }

                    YamlConfiguration loadConfiguration = YamlConfiguration.loadConfiguration(reader);
                    this.config.addDefaults(loadConfiguration);
                    this.config.options().copyDefaults(true);
                    if (comments) {
                        save();
                    } else {
                        this.config.save(file);
                    }
                }
                this.config.load(this.file);
            }
        } catch (IOException | InvalidConfigurationException ignored) {
        }
    }

    public void reload() {
        try {
            this.config.load(this.file);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }

    public InputStream getConfigContent(Reader reader) {
        try {
            String addLine, currentLine, pluginName = javaPlugin.getDescription().getName();
            int commentNum = 0;
            StringBuilder whole = new StringBuilder();
            BufferedReader bufferedReader = new BufferedReader(reader);
            while ((currentLine = bufferedReader.readLine()) != null) {
                if (currentLine.startsWith("#")) {
                    addLine = currentLine.replaceFirst("#", pluginName + "_COMMENT_" + commentNum + ":");
                    whole.append(addLine).append("\n");
                    commentNum++;
                } else {
                    whole.append(currentLine).append("\n");
                }
            }
            String config = whole.toString();
            InputStream configStream = new ByteArrayInputStream(config.getBytes(StandardCharsets.UTF_8));
            bufferedReader.close();
            return configStream;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }


    private String prepareConfigString(String configString) {
        String[] lines = configString.split("\n");
        StringBuilder config = new StringBuilder();
        for (String line : lines) {
            if (line.startsWith(javaPlugin.getDescription().getName() + "_COMMENT")) {
                String comment = "#" + line.trim().substring(line.indexOf(":") + 1);
                String normalComment;

                if (comment.startsWith("# ' ")) {
                    normalComment = comment.substring(0, comment.length() - 1).replaceFirst("# ' ", "# ");
                } else {
                    normalComment = comment;
                }

                config.append(normalComment).append("\n");
            } else {
                config.append(line).append("\n");
            }
        }
        return config.toString();
    }

    public void save() {
        if (comments) {
            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(file));
                writer.write(prepareConfigString(config.saveToString()));
                writer.flush();
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                this.config.save(file);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public String getString(String path) {
        if (config.getString(path) == null) {
            return "No path found " + path;
        }
        return StellarProtect.getInstance().getColorUtils().color(this.config.getString(path));
    }

    public String getStringOrDefault(String path, String def) {
        if (config.isSet(path)) {
            return getString(path);
        }
        set(path, def);
        save();
        return def;
    }

    public int getInt(String path) {
        return this.config.getInt(path);
    }

    public int getIntOrDefault(String path, int def) {
        if (config.isSet(path)) {
            return getInt(path);
        }
        set(path, def);
        save();
        return def;
    }

    public long getLong(String path) {
        return this.config.getLong(path);
    }

    public long getLongOrDefault(String path, long def) {
        if (config.isSet(path)) {
            return getLong(path);
        }
        set(path, def);
        save();
        return def;
    }

    public double getDouble(String path) {
        return this.config.getInt(path);
    }

    public double getDoubleOrDefault(String path, double def) {
        if (config.isSet(path)) {
            return getDouble(path);
        }
        set(path, def);
        save();
        return def;
    }

    public List<String> getList(String path) {
        return this.config.getStringList(path);
    }

    public List<String> getListOrDefault(String path, List<String> def) {
        if (config.isSet(path)) {
            return getList(path);
        }
        set(path, def);
        save();
        return def;
    }

    public boolean isSet(String path) {
        return this.config.isSet(path);
    }

    public void set(String path, Object o) {
        this.config.set(path, o);
    }

    public boolean getBoolean(String path) {
        return this.config.getBoolean(path);
    }

    public boolean getBooleanOrDefault(String path, boolean def) {
        if (config.isSet(path)) {
            return getBoolean(path);
        }
        set(path, def);
        save();
        return def;
    }

}