import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class Klient {
    public void start() throws IOException {
        Scanner scanner = new Scanner(System.in);
        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.connect(new InetSocketAddress("localhost", 8090));

        System.out.print("Type the message: ");
        String dataToSend = scanner.nextLine();

        ByteBuffer in = ByteBuffer.allocate(48);

        in.clear();
        in.put(dataToSend.getBytes());
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
        String received = new String(encoded, StandardCharsets.UTF_8).trim();
        System.out.println("Data received from server: " + received);
        System.out.print("Press enter to continue...");
        scanner.nextLine();

        socketChannel.close();
    }
}
