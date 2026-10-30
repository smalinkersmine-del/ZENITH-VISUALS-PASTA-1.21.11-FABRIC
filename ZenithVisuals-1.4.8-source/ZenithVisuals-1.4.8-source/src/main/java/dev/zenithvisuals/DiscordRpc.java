package dev.zenithvisuals;

import java.io.*;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

final class DiscordRpc {
    private static final AtomicBoolean RUNNING = new AtomicBoolean(false);
    private DiscordRpc() {}

    static void startIfConfigured() {
        if (!ZenithVisuals.enabled("discord_presence")) return;
        String id = ZenithVisuals.discordAppId == null ? "" : ZenithVisuals.discordAppId.trim();
        if (!id.matches("[0-9]{10,25}")) return;
        if (!RUNNING.compareAndSet(false, true)) return;
        Thread t = new Thread(() -> connectLoop(id), "ZenithVisuals-DiscordRPC");
        t.setDaemon(true);
        t.start();
    }

    private static void connectLoop(String appId) {
        while (true) {
            try (Transport transport = connect()) {
                if (transport == null) throw new IOException("Discord IPC not found");
                String handshake = "{\"v\":1,\"client_id\":\"" + escape(appId) + "\"}";
                send(transport.out, 0, handshake);
                readFrame(transport.in);
                String nonce = UUID.randomUUID().toString();
                String activity = "{\"cmd\":\"SET_ACTIVITY\",\"args\":{\"pid\":" + ProcessHandle.current().pid()
                        + ",\"activity\":{\"details\":\"Playing Minecraft 1.21.11\",\"state\":\"ZenithVisuals enabled\",\"timestamps\":{\"start\":" + (System.currentTimeMillis()/1000L) + "},\"instance\":false}},\"nonce\":\"" + nonce + "\"}";
                send(transport.out, 1, activity);
                transport.out.flush();
                while (true) {
                    Frame frame = readFrame(transport.in);
                    if (frame == null) break;
                    if (frame.opcode == 3) send(transport.out, 4, frame.body);
                    else if (frame.opcode == 2) break;
                }
            } catch (Throwable ignored) {
                try { Thread.sleep(4500L); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
            }
        }
    }

    private static Transport connect() throws IOException {
        if (System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")) {
            for (int i = 0; i < 10; i++) {
                String pipe = "\\\\.\\pipe\\discord-ipc-" + i;
                try {
                    FileOutputStream out = new FileOutputStream(pipe);
                    FileInputStream in = new FileInputStream(pipe);
                    return new Transport(in, out, null);
                } catch (IOException ignored) { }
            }
        } else {
            List<Path> candidates = new ArrayList<>();
            candidates.add(Paths.get("/tmp/discord-ipc-0"));
            candidates.add(Paths.get("/run/user", Integer.toString(uid()), "discord-ipc-0"));
            Path run = Paths.get("/run/user");
            if (Files.isDirectory(run)) {
                try (var dirs = Files.list(run)) {
                    dirs.forEach(d -> { for (int i = 0; i < 10; i++) candidates.add(d.resolve("discord-ipc-" + i)); });
                } catch (IOException ignored) { }
            }
            for (Path path : candidates) {
                if (!Files.exists(path)) continue;
                try {
                    SocketChannel channel = SocketChannel.open(StandardProtocolFamily.UNIX);
                    channel.connect(UnixDomainSocketAddress.of(path));
                    return new Transport(Channels.newInputStream(channel), Channels.newOutputStream(channel), channel);
                } catch (IOException ignored) { }
            }
        }
        return null;
    }

    private static int uid() {
        String configured = System.getenv("UID");
        if (configured != null) try { return Integer.parseInt(configured); } catch (NumberFormatException ignored) { }
        return 1000;
    }

    private static void send(OutputStream out, int opcode, String payload) throws IOException {
        byte[] data = payload.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        ByteBuffer header = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putInt(opcode).putInt(data.length);
        out.write(header.array());
        out.write(data);
        out.flush();
    }

    private static Frame readFrame(InputStream in) throws IOException {
        byte[] header = in.readNBytes(8);
        if (header.length != 8) return null;
        ByteBuffer b = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN);
        int opcode = b.getInt();
        int len = b.getInt();
        if (len < 0 || len > 1_000_000) throw new IOException("Invalid Discord IPC frame");
        byte[] body = in.readNBytes(len);
        if (body.length != len) return null;
        return new Frame(opcode, new String(body, java.nio.charset.StandardCharsets.UTF_8));
    }

    private static String escape(String s) { return s.replace("\\", "\\\\").replace("\"", "\\\""); }
    private record Frame(int opcode, String body) {}
    private static final class Transport implements AutoCloseable {
        final InputStream in; final OutputStream out; final SocketChannel channel;
        Transport(InputStream in, OutputStream out, SocketChannel channel) { this.in=in; this.out=out; this.channel=channel; }
        public void close() throws IOException { try { in.close(); } finally { try { out.close(); } finally { if(channel != null) channel.close(); } } }
    }
}
