package com.civrealms.plugin.bukkit.shard;

import com.civrealms.plugin.bukkit.BungeeMessenger;
import com.civrealms.plugin.bukkit.inventory.GZIPInventorySerializer;
import com.civrealms.plugin.bukkit.inventory.log.InventoryLogger;
import com.civrealms.plugin.common.packet.PacketReceiveEvent;
import com.civrealms.plugin.common.packet.PacketSender;
import com.civrealms.plugin.common.packets.PacketPlayerTransfer;
import com.civrealms.plugin.common.packets.PacketRequestShards;
import com.civrealms.plugin.common.packets.PacketShardInfo;
import com.civrealms.plugin.common.packets.data.BoatData;
import com.civrealms.plugin.common.shard.AquaNether;
import com.civrealms.plugin.common.shard.Shard;
import com.google.common.eventbus.Subscribe;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public class ShardManager {
  private final Plugin scheduler;
  private final InventoryLogger logger;
  private final String currentShard;
  private final BungeeMessenger messenger;
  private final PacketSender sender;
  private final LeaveShardManager leaveShardManager;
  private boolean shardInfo = false;
  private String transitiveShard;
  private String deathShard;
  private AquaNether aquaNether;
  private Collection<Shard> shards;

  public ShardManager(Plugin scheduler,
      InventoryLogger logger, String currentShard,
      BungeeMessenger messenger,
      PacketSender sender, LeaveShardManager leaveShardManager) {
    this.scheduler = scheduler;
    this.logger = logger;
    this.currentShard = currentShard;
    this.messenger = messenger;
    this.sender = sender;
    this.leaveShardManager = leaveShardManager;
  }

  public void sendIdentify(boolean onJoin) {
    // don't send the identify packet when a player joins if we already have the shard info
    if (!hasShardInfo() || !onJoin) {
      System.out.println("IDENTIFYING TO PROXY AS >> " + currentShard);
      sender.sendProxy(new PacketRequestShards(currentShard));
    }
  }

  private void readShards(PacketShardInfo packet) {
    this.transitiveShard = packet.getTransitiveShard();
    this.deathShard = packet.getDeathShard();
    this.aquaNether = packet.getAquaNether();
    this.shards = new HashSet<>(packet.getShards());
    this.shardInfo = true;
  }

  public void sendPlayer(Player player, PacketPlayerTransfer.TeleportCause cause, BoatData boat, String server, Location loc) {
    Objects.requireNonNull(player);
    Objects.requireNonNull(cause);
    Objects.requireNonNull(server);

    if (loc == null) {
      loc = player.getLocation();
    }

    leaveShardManager.setLeaving(player);

    // Send to proxy to store so it can do something later
    // we can't send a message directly to the other server because
    // of the risk that it won't have any players online -
    // servers with no players cannot send nor receive plugin messages
    // ^ THIS IS WRONG BUNGEE IS CLEVER IT USES A QUEUE
    // IGNORE THAT GUY WE'RE SENDING IT STRAIGHT TO THE OTHER SERVER!
    // the guy before me also sucks we're using rabbitmq now. screw bungee's plugin messaging.

    // TODO possible dupe: players can drop items etc and dupe in the time between when this packet is sent and acknowledgement is received
    // option 1: prevent players dropping items / moving them in their inventory - Cancel inventorydropevent, inventorypickupevent, and inventoryclickevent
    // option 2: send the inventory later - potentially issues with bungee still not sending the player fast enough

    sender.send(server, new PacketPlayerTransfer(
        cause,
        boat,
        player.getUniqueId(),
        new GZIPInventorySerializer().serialize(player.getInventory().getContents()),
        loc.getX(),
        loc.getY(),
        loc.getZ(),
        loc.getYaw(),
        loc.getPitch(),
        player.getRemainingAir(),
        player.getHealth(),
        player.getExp(),
        player.getLevel(),
        player.getExhaustion(),
        player.getSaturation(),
        player.getFoodLevel(),
        player.getInventory().getHeldItemSlot(),
        player.getGameMode().getValue()
    ), () -> {
      // success
      logger.log(player, cause.name() + "_TO_" + server);
      // connect the player
      messenger.connect(player, server);
      // clear inventory
      player.getInventory().setContents(new ItemStack[player.getInventory().getContents().length]);
    }, () -> {
      // failure :(
      player.sendMessage(ChatColor.RED + "Failed to connect to server.");
    });
  }

  @Subscribe
  public void onShardsReceive(PacketReceiveEvent e) {
    if (!(e.getPacket() instanceof PacketShardInfo)) {
      return;
    }
    PacketShardInfo packet = (PacketShardInfo) e.getPacket();

    System.out.println("READ SHARDS # >> " + packet.getShards().size());
    readShards(packet);
  }

  public String getCurrentShard() {
    return currentShard;
  }

  public Collection<Shard> getShards() {
    return shards;
  }

  public AquaNether getAquaNether() {
    return aquaNether;
  }

  public String getTransitiveShard() {
    return transitiveShard;
  }

  public String getDeathShard() {
    return deathShard;
  }

  public String getShard(int x, int y, int z) {
    if (shards == null || shards.isEmpty()) {
      return null;
    }
    Shard destShard = null;
    for (Shard shard : shards) {
      if (shard.contains(x, y, z)) {
        destShard = shard;
        break;
      }
    }

    if (destShard == null) {
      return transitiveShard;
    } else {
      return destShard.getServer();
    }
  }

  public boolean hasShardInfo() {
    return shardInfo;
  }
}
