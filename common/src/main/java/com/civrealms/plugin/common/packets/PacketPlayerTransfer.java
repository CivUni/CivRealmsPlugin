package com.civrealms.plugin.common.packets;

import com.civrealms.plugin.common.packet.Packet;
import com.civrealms.plugin.common.packets.data.BoatData;
import com.civrealms.plugin.common.packets.stream.DataInputStream;
import com.civrealms.plugin.common.packets.stream.DataOutputStream;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public class PacketPlayerTransfer implements Packet {
  private TeleportCause cause;
  private BoatData boat;
  private UUID uniqueId;
  private byte[] inventorySerial;
  private double x;
  private double y;
  private double z;
  private float yaw;
  private float pitch;
  private int air;
  private double health;
  private float xp;
  private int levels;
  private float exhaustion;
  private float saturation;
  private int food;
  private int hotbar;
  private int gamemode;

  @Override
  public void read(DataInputStream in) {
    cause = TeleportCause.values()[in.readByte()];

    boolean hasBoat = in.readBoolean();
    if (hasBoat) {
      UUID boatId = in.readUUID();
      boolean passenger = in.readBoolean();
      byte species = in.readByte();
      boat = new BoatData(boatId, passenger, species);
    } else {
      boat = null;
    }

    uniqueId = in.readUUID();
    System.out.println("FOR " + uniqueId + ">> BOAT " + boat);
    inventorySerial = in.readByteArray();
    x = in.readDouble();
    y = in.readDouble();
    z = in.readDouble();
    yaw = in.readFloat();
    pitch = in.readFloat();
    air = in.readInt();
    health = in.readDouble();
    xp = in.readFloat();
    levels = in.readInt();
    exhaustion = in.readFloat();
    saturation = in.readFloat();
    food = in.readByte();
    hotbar = in.readByte();
    gamemode = in.readByte();
  }

  @Override
  public void write(DataOutputStream out) {
    out.writeByte((byte) cause.ordinal());

    if (boat == null) {
      out.writeBoolean(false);
    } else {
      out.writeBoolean(true);
      out.writeUUID(boat.getBoat());
      out.writeBoolean(boat.isPassenger());
      out.writeByte(boat.getSpecies());
    }

    out.writeUUID(uniqueId);
    out.writeByteArray(inventorySerial);
    out.writeDouble(x);
    out.writeDouble(y);
    out.writeDouble(z);
    out.writeFloat(yaw);
    out.writeFloat(pitch);
    out.writeInt(air);
    out.writeDouble(health);
    out.writeFloat(xp);
    out.writeInt(levels);
    out.writeFloat(exhaustion);
    out.writeFloat(saturation);
    out.writeByte(food);
    out.writeByte(hotbar);
    out.writeByte(gamemode);
    System.out.println("WRITING " + uniqueId + " >> BOAT " + boat + " >> CAUSE " + cause);
  }

  public enum TeleportCause {
    TRANSITIVE,
    TO_AN,
    FROM_AN,
    DEATH
  }
}
