package dev.luckynetwork.alviann.luckyinjector.spigot;

import com.github.alviannn.sqlhelper.SQLBuilder;
import com.github.alviannn.sqlhelper.SQLHelper;
import dev.luckynetwork.alviann.luckyinjector.loader.Loader;
import dev.luckynetwork.alviann.luckyinjector.spigot.commands.MainCMD;
import lombok.Getter;
import lombok.SneakyThrows;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class SpigotInjector extends JavaPlugin {

    @Getter
    private static SpigotInjector instance;

    @SneakyThrows
    @Override
    public void onEnable() {
        instance = this;

        SpigotInjector.loadEarly();

        Loader.initConfig(SpigotInjector.class);
        this.reloadConfig();

        this.getCommand("luckyinjector").setExecutor(new MainCMD());
    }

    /**
     * loads the injector earlier
     */
    public static void loadEarly() throws Exception {
        Loader.startInjecting(SpigotInjector.class);
        Loader.initConfig(SpigotInjector.class);
    }

    /**
     * gets the default SQLBuilder instance with the default values (host, port, username, and password)
     * <p>
     * NOTE: this isn't finished yet as the database is empty and you need to fill it out alone like on the code below
     * <p>
     * <pre><code>
     *     SQLBuilder builder = ...;
     *     builder.setDatabase("the database name");
     * </code></pre>
     */
    @SneakyThrows
    public static SQLBuilder getDefaultSQLBuilder() {
        Loader.initConfig(SpigotInjector.class);

        File configFile = Loader.CONFIG_FILE;
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        return SQLHelper.newBuilder(SQLHelper.Type.MYSQL)
                .setHost(config.getString("sql.host"))
                .setPort(config.getString("sql.port"))
                .setUsername(config.getString("sql.username"))
                .setPassword(config.getString("sql.password"))
                .setHikari(true);
    }

    /**
     * checks if the auto-update feature is enabled,
     * by default it's {@code true}
     */
    public boolean isAutoUpdate() {
        return this.getConfig().getBoolean("auto-update", true);
    }

    /**
     * Discards any data in {@link #getConfig()} and reloads from disk.
     */
    @Override
    public void reloadConfig() {
        super.reloadConfig();

        if (!this.getConfig().contains("auto-update")) {
            this.getConfig().set("auto-update", true);

            this.saveConfig();
            super.reloadConfig();
        }
    }
}
