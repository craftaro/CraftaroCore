package com.songoda.core.nms.entity;

import org.bukkit.entity.Player;

public interface NMSPlayer {
    void sendPacket(Player player, Object packet);
}