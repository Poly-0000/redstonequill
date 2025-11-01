/*
 * @file Networking.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Main client/server message handling.
 */
package wile.redstonepen.libmc;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.network.PacketDistributor;

public class NetworkingClient
{
    public static void clientInit()
    {
    }
    @OnlyIn(Dist.CLIENT)
    private static void send(String packet_id, CompoundTag payload_nbt)
    {
        PacketDistributor.sendToServer(new Networking.UnifiedPayload(new Networking.UnifiedPayload.UnifiedData(packet_id, payload_nbt)));
    }
    @OnlyIn(Dist.CLIENT)
    public static class PacketTileNotifyClientToServer extends Networking.PacketTileNotifyClientToServer
    {

    }
    @OnlyIn(Dist.CLIENT)
    public static class PacketContainerSyncClientToServer extends Networking.PacketContainerSyncClientToServer
    {
    }
    @OnlyIn(Dist.CLIENT)
    public static class PacketNbtNotifyClientToServer extends Networking.PacketNbtNotifyClientToServer
    {
    }
}