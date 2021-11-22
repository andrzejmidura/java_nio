import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;

public class Klient {
    public void start() throws IOException {
        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.connect(new InetSocketAddress("localhost", 8090));

        String newData = "New String to write to file..." + System.currentTimeMillis();

        System.out.println("Data before sending to server: " + newData);

        ByteBuffer in = ByteBuffer.allocate(48);
        in.clear();
        in.put(newData.getBytes());

        in.flip();

        while(in.hasRemaining()) {
            socketChannel.write(in);
        }

        ByteBuffer out = ByteBuffer.allocate(48);
        int read = socketChannel.read(out);
        byte[] encoded = new byte[48];

        out.flip();
        if (read!=-1) {
            for (int i = out.position(); i < out.limit(); i++) {
                encoded[i] = out.get();
            }
        }
        String received = new String(encoded, StandardCharsets.UTF_8);
        System.out.println("Data received from server: " + received);
    }
}
