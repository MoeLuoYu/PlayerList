package xyz.moeluoyu.velocitypowered.playerlist;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

@Plugin(
        id = "playerlist",
        name = "PlayerList",
        version = "1.0",
        description = "修改 Minecraft 服务器 ping 信息中的 PlayerList",
        authors = {"MoeLuoYu"}
)
public class Main {

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;
    private Properties config;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    @Inject
    public Main(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        loadConfig();
        logger.info("定制插件找落雨，买插件上速德优，速德优（北京）网络科技有限公司出品，落雨QQ：1498640871");
        registerCommand();
        // 获取事件管理器
        EventManager eventManager = server.getEventManager();
        // 注册事件监听器
        //noinspection deprecation
        eventManager.register(
                this,
                ProxyPingEvent.class,
                PostOrder.LATE,
                this::onProxyPing
        );
    }

    private void onProxyPing(ProxyPingEvent event) {
        ServerPing originalPing = event.getPing();
        String playerListText = config.getProperty("playerlist.text", "默认 PlayerList 文本");
        String[] lines = playerListText.split("\n");

        List<ServerPing.SamplePlayer> samplePlayers = new ArrayList<>();
        for (String line : lines) {
            Component lineComponent = parseText(line);
            String lineString = LegacyComponentSerializer.legacySection().serialize(lineComponent);
            UUID uuid = UUID.randomUUID();
            samplePlayers.add(new ServerPing.SamplePlayer(lineString, uuid));
        }

        ServerPing.Builder pingBuilder = originalPing.asBuilder();
        pingBuilder.samplePlayers(samplePlayers.toArray(ServerPing.SamplePlayer[]::new));

        event.setPing(pingBuilder.build());
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        logger.info("插件已关闭，感谢您的使用！");
    }

    private void loadConfig() {
        try {
            File configFile = dataDirectory.resolve("config.properties").toFile();
            if (!configFile.exists()) {
                Files.createDirectories(dataDirectory);
                Files.createFile(configFile.toPath());
                Properties defaultConfig = new Properties();
                defaultConfig.setProperty("playerlist.text", "<#FF0000>Line 1 这是16进制颜色文本\nLine 2 这是无颜色文本\n&aLine 3 这是传统颜色文本");
                try (var outputStream = new java.io.FileOutputStream(configFile);
                     var writer = new java.io.OutputStreamWriter(outputStream, java.nio.charset.StandardCharsets.UTF_8)) {
                    defaultConfig.store(writer, "PlayerList Config");
                }
            }
            config = new Properties();
            try (var inputStream = Files.newInputStream(configFile.toPath());
                 var reader = new java.io.InputStreamReader(inputStream, java.nio.charset.StandardCharsets.UTF_8)) {
                config.load(reader);
            }
            logger.info("配置文件加载成功。");
        } catch (IOException e) {
            logger.error("加载配置文件失败：", e);
        }
    }

    private Component parseText(String text) {
        if (text.contains("<")) {
            return miniMessage.deserialize(text);
        } else {
            return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
        }
    }

    private void registerCommand() {
        CommandMeta commandMeta = server.getCommandManager().metaBuilder("playerlistreload")
                .aliases("plr")
                .build();
        SimpleCommand command = invocation -> {
            loadConfig();
            invocation.source().sendMessage(Component.text("PlayerList 配置文件重新加载成功。"));
        };
        server.getCommandManager().register(commandMeta, command);
    }
}