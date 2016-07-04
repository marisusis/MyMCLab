package me.megamichiel.mymclab.packet;

import me.megamichiel.mymclab.io.ProtocolInput;
import me.megamichiel.mymclab.io.ProtocolOutput;
import me.megamichiel.mymclab.perm.DefaultPermission;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

public class PermissionPacket extends Packet {

    private static final byte ID = getId(PermissionPacket.class);

    private final Collection<DefaultPermission> permissions;

    public PermissionPacket(Collection<DefaultPermission> permissions) {
        super(ID);
        this.permissions = permissions;
    }

    public PermissionPacket(ProtocolInput data) throws IOException {
        super(ID);
        DefaultPermission[] perms = new DefaultPermission[data.readUnsignedByte()];
        for (int i = 0; i < perms.length; i++)
            perms[i] = data.readEnum(DefaultPermission.class);
        permissions = Arrays.asList(perms);
    }

    public Collection<DefaultPermission> getPermissions() {
        return permissions;
    }

    @Override
    protected void encode(ProtocolOutput data) throws IOException {
        data.writeByte(permissions.size());
        for (DefaultPermission perm : permissions) data.writeEnum(perm);
    }
}
