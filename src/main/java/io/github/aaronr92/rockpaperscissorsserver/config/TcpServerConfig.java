package io.github.aaronr92.rockpaperscissorsserver.config;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Server;
import io.github.aaronr92.rockpaperscissorsserver.listener.PacketListener;
import io.github.aaronr92.rockpaperscissorsserver.packet.client.ClientboundConnectionPacket;
import io.github.aaronr92.rockpaperscissorsserver.packet.client.ClientboundGameStartPacket;
import io.github.aaronr92.rockpaperscissorsserver.packet.client.ClientboundPlayerGameStepActionPacket;
import io.github.aaronr92.rockpaperscissorsserver.packet.server.*;
import io.github.aaronr92.rockpaperscissorsserver.util.FinishState;
import io.github.aaronr92.rockpaperscissorsserver.util.GameStepAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class TcpServerConfig {

    @Value("${application.tcp}")
    private int tcpPort;

    private final PacketListener packetListener;

    private final Logger LOG = LoggerFactory.getLogger(TcpServerConfig.class);

    public TcpServerConfig(PacketListener packetListener) {
        this.packetListener = packetListener;
    }

    @Bean
    public Server getServer() {
        Server server = new com.esotericsoftware.kryonet.Server();
        Kryo kryo = server.getKryo();

        // Classes
        kryo.register(GameStepAction.class);
        kryo.register(FinishState.class);

        // Clientbound packets
        kryo.register(ClientboundConnectionPacket.class);
        kryo.register(ClientboundGameStartPacket.class);
        kryo.register(ClientboundPlayerGameStepActionPacket.class);

        // Serverbount packets
        kryo.register(ServerboundConnectionPacket.class);
        kryo.register(ServerboundGameEndPacket.class);
        kryo.register(ServerboundGameStartPacket.class);
        kryo.register(ServerboundRemainingTimePacket.class);
        kryo.register(ServerboundServerChoicePacket.class);

        server.addListener(packetListener);

        try {
            server.bind(tcpPort);
            server.start();
            LOG.info("TCP server started");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return server;
    }

}
