package me.megamichiel.mymclab.packet;

import me.megamichiel.mymclab.io.ProtocolInput;
import me.megamichiel.mymclab.io.ProtocolOutput;
import me.megamichiel.mymclab.util.ColoredText;

import java.io.IOException;

public class ServerStatusPacket extends Packet {

    private static final byte ID = getId(ServerStatusPacket.class);

    private final long pingId;

    private final int versionDiff;
    private final ColoredText motd;
    private final int playerCount;

    public ServerStatusPacket(long pingId, int versionDiff,
                              ColoredText motd, int playerCount) {
        super(ID);
        this.pingId = pingId;
        this.versionDiff = versionDiff;
        this.motd = motd;
        this.playerCount = playerCount;
    }

    public ServerStatusPacket(ProtocolInput stream) throws IOException {
        super(ID);
        pingId = stream.readVarLong();
        versionDiff = stream.readByte();
        motd = new ColoredText(stream);
        playerCount = stream.readVarInt();
    }

    @Override
    protected void encode(ProtocolOutput data) throws IOException {
        data.writeVarLong(pingId);
        data.writeByte(versionDiff);
        motd.write(data);
        data.writeVarInt(playerCount);
    }

    public long getPingId() {
        return pingId;
    }

    public int getVersionDiff() {
        return versionDiff;
    }

    public ColoredText getMotd() {
        return motd;
    }

    public int getPlayerCount() {
        return playerCount;
    }
}
