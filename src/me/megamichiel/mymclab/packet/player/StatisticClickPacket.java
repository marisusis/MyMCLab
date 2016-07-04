package me.megamichiel.mymclab.packet.player;

import me.megamichiel.mymclab.io.ProtocolInput;
import me.megamichiel.mymclab.io.ProtocolOutput;
import me.megamichiel.mymclab.packet.Packet;
import me.megamichiel.mymclab.perm.DefaultPermission;

import java.io.IOException;

public class StatisticClickPacket extends Packet {

    private static final byte ID = getId(StatisticClickPacket.class);

    private final String name;
    private final int itemIndex;

    public StatisticClickPacket(String name, int itemIndex) {
        super(ID);
        this.name = name;
        this.itemIndex = itemIndex;
    }

    public StatisticClickPacket(ProtocolInput data) throws IOException {
        super(ID);
        name = data.readString();
        itemIndex = data.readUnsignedShort();
    }

    public String getName() {
        return name;
    }

    public int getItemIndex() {
        return itemIndex;
    }

    @Override
    protected void encode(ProtocolOutput data) throws IOException {
        data.writeString(name);
        data.writeShort(itemIndex);
    }

    @Override
    public DefaultPermission getPermission() {
        return name == null ? DefaultPermission.CLICK_SERVER_INFO : DefaultPermission.CLICK_PLAYERS;
    }
}
