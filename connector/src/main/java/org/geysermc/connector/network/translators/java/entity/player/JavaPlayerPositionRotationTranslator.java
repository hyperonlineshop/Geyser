/*
 * Copyright (c) 2019 GeyserMC. http://geysermc.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * @author GeyserMC
 * @link https://github.com/GeyserMC/Geyser
 */

package org.geysermc.connector.network.translators.java.entity.player;

import com.flowpowered.math.vector.Vector3f;
import com.github.steveice10.mc.protocol.packet.ingame.client.world.ClientTeleportConfirmPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerJoinGamePacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.player.ServerPlayerPositionRotationPacket;
import com.nukkitx.protocol.bedrock.packet.MovePlayerPacket;
import com.nukkitx.protocol.bedrock.packet.PlayStatusPacket;
import com.nukkitx.protocol.bedrock.packet.SetEntityDataPacket;
import com.nukkitx.protocol.bedrock.packet.SetPlayerGameTypePacket;
import org.geysermc.connector.console.GeyserLogger;
import org.geysermc.connector.entity.Entity;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;

public class JavaPlayerPositionRotationTranslator extends PacketTranslator<ServerPlayerPositionRotationPacket> {

    @Override
    public void translate(ServerPlayerPositionRotationPacket packet, GeyserSession session) {
        Entity entity = session.getPlayerEntity();
        if (entity == null)
            return;

        if (!session.isLoggedIn())
            return;

        if (!session.isSpawned()) {
            ServerJoinGamePacket javaJoinPacket = (ServerJoinGamePacket) session.getJavaPacketCache().getCachedValues().remove("java_join_packet");

            PlayStatusPacket playStatus = new PlayStatusPacket();
            playStatus.setStatus(PlayStatusPacket.Status.LOGIN_SUCCESS);
            session.getUpstream().sendPacketImmediately(playStatus);

            entity.moveAbsolute(new Vector3f(packet.getX(), packet.getY(), packet.getZ()), packet.getPitch(), packet.getYaw());

            SetPlayerGameTypePacket playerGameTypePacket = new SetPlayerGameTypePacket();
            playerGameTypePacket.setGamemode(javaJoinPacket.getGameMode().ordinal());
            session.getUpstream().sendPacket(playerGameTypePacket);

            SetEntityDataPacket entityDataPacket = new SetEntityDataPacket();
            entityDataPacket.setRuntimeEntityId(entity.getGeyserId());
            entityDataPacket.getMetadata().putAll(entity.getMetadata());

            session.getUpstream().sendPacket(entityDataPacket);
            session.getPlayerEntity().setEntityId(javaJoinPacket.getEntityId());

            MovePlayerPacket movePlayerPacket = new MovePlayerPacket();
            movePlayerPacket.setRuntimeEntityId(entity.getGeyserId());
            movePlayerPacket.setPosition(new Vector3f(packet.getX(), packet.getY(), packet.getZ()));
            movePlayerPacket.setRotation(new Vector3f(packet.getPitch(), packet.getYaw(), 0));
            movePlayerPacket.setMode(MovePlayerPacket.Mode.NORMAL);
            movePlayerPacket.setOnGround(true);
            entity.setMovePending(false);

            session.getUpstream().sendPacket(movePlayerPacket);
            session.setSpawned(true);

            GeyserLogger.DEFAULT.info("Spawned player at " + packet.getX() + " " + packet.getY() + " " + packet.getZ());
        }

        entity.moveAbsolute(new Vector3f(packet.getX(), packet.getY(), packet.getZ()), packet.getPitch(), packet.getYaw());

        MovePlayerPacket movePlayerPacket = new MovePlayerPacket();
        movePlayerPacket.setRuntimeEntityId(entity.getGeyserId());
        movePlayerPacket.setPosition(new Vector3f(packet.getX(), packet.getY(), packet.getZ()));
        movePlayerPacket.setRotation(new Vector3f(packet.getPitch(), packet.getYaw(), 0));
        movePlayerPacket.setMode(MovePlayerPacket.Mode.NORMAL);
        movePlayerPacket.setOnGround(true);
        entity.setMovePending(false);

        session.getUpstream().sendPacket(movePlayerPacket);
        session.setSpawned(true);

        ClientTeleportConfirmPacket teleportConfirmPacket = new ClientTeleportConfirmPacket(packet.getTeleportId());
        session.getDownstream().getSession().send(teleportConfirmPacket);
    }
}
