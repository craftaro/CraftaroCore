package com.songoda.core.compatibility.client;

import com.songoda.core.compatibility.server.ServerVersion;
import com.viaversion.viaversion.api.Via;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.UUID;

/**
 * Handy reference for checking a connected client's Minecraft version<br>
 * NOTE: this is automatically initialized through SongodaCore
 */
public class ClientVersion {
    static HashMap<UUID, ServerVersion> players = new HashMap<>();

    /**
     * Check to see what client version this player is connected to the server
     * with. Note that if a player is connecting with a newer client than the server,
     * this value will simply be the server version.
     *
     * @param player Player to check
     *
     * @return ServerVersion that matches this player's Minecraft version
     */
    public static ServerVersion getClientVersion(Player player) {
        if (player == null || !players.containsKey(player.getUniqueId())) {
            return ServerVersion.getServerVersion();
        }

        return players.get(player.getUniqueId());
    }

    /**
     * Do Not Use: This is handled by SongodaCore.
     */
    @Deprecated
    public static void onLoginProtocol(Player p, JavaPlugin plugin) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (p.isOnline()) {
                final int version = protocolsupport.api.ProtocolSupportAPI.getProtocolVersion(p).getId();
                players.put(p.getUniqueId(), protocolToVersion(version));
            }
        }, 20);
    }

    /**
     * Do Not Use: This is handled by SongodaCore.
     */
    @Deprecated
    public static void onLoginVia(Player p, JavaPlugin plugin) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (p.isOnline()) {
                final int version = Via.getAPI().getPlayerVersion(p.getUniqueId());
                players.put(p.getUniqueId(), protocolToVersion(version));
            }
        }, 20);
    }

    private static ServerVersion protocolToVersion(int version) {
        // https://wiki.vg/Protocol_version_numbers#Versions_after_the_Netty_rewrite
        // https://github.com/ViaVersion/ViaVersion/blob/master/api/src/main/java/com/viaversion/viaversion/api/protocol/version/ProtocolVersion.java
        switch (version) {
            case 4:
            case 5:
                return ServerVersion.V1_7;
            case 47:
                return ServerVersion.V1_8;
            case 107:
            case 108:
            case 109:
            case 110:
                return ServerVersion.V1_9;
            case 210:
                return ServerVersion.V1_10;
            case 315:
            case 316:
                return ServerVersion.V1_11;
            case 335:
            case 338:
            case 340:
                return ServerVersion.V1_12;
            case 393:
            case 401:
            case 404:
                return ServerVersion.V1_13;
            case 477:
            case 485:
            case 490:
            case 498:
                return ServerVersion.V1_14;
            case 573:
            case 575:
            case 578:
                return ServerVersion.V1_15;
            case 735:
            case 736:
            case 751:
            case 753:
            case 754:
                return ServerVersion.V1_16;
            case 755:
            case 756:
                return ServerVersion.V1_17;

            default:
                return version > 756 ? ServerVersion.getServerVersion() : ServerVersion.UNKNOWN;
        }
    }

    /**
     * Do Not Use: This is handled by SongodaCore.
     */
    @Deprecated
    public static void onLogout(Player p) {
        players.remove(p.getUniqueId());
    }
}
