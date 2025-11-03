/*
 * @file Networking.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Main client/server message handling.
 */
package wfphantom.redstonequill.libmc;

import net.minecraft.client.player.LocalPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import wfphantom.redstonequill.ModConstants;

public class Networking {
    public static void init(net.neoforged.neoforge.network.registration.PayloadRegistrar registrar) {
        registrar.playBidirectional(UnifiedPayload.TYPE, UnifiedPayload.STREAM_CODEC, (unifed_payload, context) -> {
            if (context.player() instanceof ServerPlayer player) {
                //final ServerPlayer player = (ServerPlayer)context.player();
                final ServerLevel world = player.serverLevel();
                final CompoundTag payload = unifed_payload.data().nbt();
                player.server.execute(() -> {
                    if (unifed_payload.data().id().equals(PacketTileNotifyClientToServer.PACKET_ID)) {
                        final BlockPos pos = BlockPos.of(payload.getLong("pos"));
                        final BlockEntity te = world.getBlockEntity(pos);
                        if (!(te instanceof IPacketTileNotifyReceiver)) return;
                        ((IPacketTileNotifyReceiver) te).onClientPacketReceived();
                    }
                });
            } else {
                final LocalPlayer player = (LocalPlayer) context.player();
                final Level world = player.level();
                final CompoundTag payload = unifed_payload.data().nbt();
                context.enqueueWork(() -> {
                    if (unifed_payload.data().id().equals(PacketTileNotifyServerToClient.PACKET_ID)) {
                        final BlockPos pos = BlockPos.of(payload.getLong("pos"));
                        final CompoundTag nbt = payload.getCompound("nbt");
                        final BlockEntity te = world.getBlockEntity(pos);
                        if (!(te instanceof IPacketTileNotifyReceiver nte)) return;
                        nte.onServerPacketReceived(nbt);
                    }
                });
            }
        });
    }

    //--------------------------------------------------------------------------------------------------------------------
    // Unified Packet Handling
    //--------------------------------------------------------------------------------------------------------------------
    public record UnifiedPayload(UnifiedData data) implements CustomPacketPayload {
        public static final StreamCodec<FriendlyByteBuf, UnifiedPayload> STREAM_CODEC = CustomPacketPayload.codec(UnifiedPayload::write, UnifiedPayload::new);
        public static final CustomPacketPayload.Type<UnifiedPayload> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ModConstants.MODID, "unpnbt"));

        private UnifiedPayload(FriendlyByteBuf buf) {
            this(new UnifiedData(buf.readUtf(), buf.readNbt()));
        }

        private void write(FriendlyByteBuf buf) {
            data.write(buf);
        }

        public CustomPacketPayload.Type<UnifiedPayload> type() {
            return TYPE;
        }

        public record UnifiedData(String id, CompoundTag nbt) {
            public void write(FriendlyByteBuf buf) {
                buf.writeUtf(id);
                buf.writeNbt(nbt);
            }

            @Override
            public String toString() {
                return id + ": " + nbt.toString();
            }
        }
    }

    private static void sendToClients(ServerLevel world, String packet_id, CompoundTag payload_nbt) {
        final var payload = new UnifiedPayload(new UnifiedPayload.UnifiedData(packet_id, payload_nbt));
        for (ServerPlayer player : world.players()) PacketDistributor.sendToPlayer(player, payload);
    }

    //--------------------------------------------------------------------------------------------------------------------
    // Tile entity notifications
    // --------------------------------------------------------------------------------------------------------------------
    public interface IPacketTileNotifyReceiver {
        default void onServerPacketReceived(CompoundTag nbt) {
        }

        default void onClientPacketReceived() {
        }
    }

    public static class PacketTileNotifyClientToServer {
        protected static final String PACKET_ID = "tnc2s";
    }

    public static class PacketTileNotifyServerToClient {
        protected static final String PACKET_ID = "tns2c";

        public static void sendToPlayers(BlockEntity te, CompoundTag nbt) {
            if ((te == null) || (!(te.getLevel() instanceof ServerLevel sworld))) return;
            final CompoundTag payload = new CompoundTag();
            payload.putLong("pos", te.getBlockPos().asLong());
            payload.put("nbt", nbt);
            sendToClients(sworld, PACKET_ID, payload);
        }
    }
}
