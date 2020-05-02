package dev.luckynetwork.alviann.luckyinjector.updater;

import com.github.alviannn.sqlhelper.utils.Closer;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Getter;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Getter
public class Updater {

    @Nullable
    private String latestVersion, latestDownloadUrl;

    /**
     * fetches for the new update in async
     */
    public CompletableFuture<Boolean> fetchUpdateAsync() {
        return CompletableFuture.supplyAsync(this::fetchUpdate);
    }

    /**
     * fetches for the new update in sync
     */
    public boolean fetchUpdate() {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30L, TimeUnit.SECONDS)
                .readTimeout(30L, TimeUnit.SECONDS)
                .build();

        Request request = new Request.Builder()
                .url("https://raw.githubusercontent.com/Alviannn/LuckyInjector/master/update-info.json")
                .header("User-Agent", "Mozilla/5.0")
                .get()
                .build();

        boolean fetchedUpdateInfo = false;
        try (Closer closer = new Closer()) {
            Response response = client.newCall(request).execute();
            ResponseBody body = closer.add(response.body());

            if (body == null)
                throw new NullPointerException("Body is null");

            Reader reader = closer.add(body.charStream());
            JsonObject updateInfo = JsonParser.parseReader(reader).getAsJsonObject();

            latestVersion = updateInfo.get("version").getAsString();
            latestDownloadUrl = updateInfo.get("download-url").getAsString();

            fetchedUpdateInfo = true;
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }

        return fetchedUpdateInfo;
    }

    /**
     * checks if the plugin is updatable
     */
    public boolean checkUpdate() {
        ClassLoader loader = this.getClass().getClassLoader();
        String currentVersion = null;

        try (Closer closer = new Closer()) {
            InputStream stream = closer.add(loader.getResourceAsStream("version.info"));

            if (stream == null)
                throw new NullPointerException("Cannot find version.info file!");

            Scanner scanner = closer.add(new Scanner(stream));
            if (scanner.hasNext())
                currentVersion = scanner.nextLine();
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }

        if (currentVersion == null)
            return true;
        if (latestVersion == null)
            return false;

        return !currentVersion.equals(latestVersion);
    }

    /**
     * starts updating the plugin
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void update(File pluginsFolder, boolean async) {
        Runnable runnable = () -> {
            boolean updateSuccess = false;

            if (latestDownloadUrl == null)
                throw new NullPointerException("Latest download URL is null");

            try (Closer closer = new Closer()) {
                URL url = new URL(latestDownloadUrl);
                InputStream stream = closer.add(url.openStream());

                if (stream == null)
                    throw new NullPointerException("Cannot fetch the stream!");

                File updatedFile = new File(pluginsFolder, url.getFile());
                // creates the plugin folder if needed
                if (!pluginsFolder.exists())
                    pluginsFolder.mkdir();

                // downloads the file
                Files.copy(stream, updatedFile.toPath());
                updateSuccess = true;
            } catch (Exception e) {
                System.err.println(e.getMessage());
            }

            if (!updateSuccess)
                return;

            File pluginFile = this.getCurrentPluginFile();
            if (pluginFile == null || !pluginFile.exists())
                return;

            // deletes the current plugin file
            // if the file is successfully updated
            pluginFile.delete();
        };

        if (async)
            CompletableFuture.runAsync(runnable);
        else
            runnable.run();
    }

    /**
     * gets the plugin file
     */
    @Nullable
    private File getCurrentPluginFile() {
        try {
            return new File(Updater.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (URISyntaxException e) {
            System.err.println(e.getMessage());
        }
        return null;
    }

}
