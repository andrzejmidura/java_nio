import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;

class Serwer {
    private final Map<SocketChannel, Queue<ByteBuffer>> pendingData = new HashMap<>();

    private ServerSocketChannel serverSocketChannel;
    private Selector selector;

    public void start() throws IOException {
        selector = Selector.open();

        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress(8090));
        serverSocketChannel.configureBlocking(false);

        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        while (true) {
            selector.select();
            Set<SelectionKey> keys = selector.selectedKeys();

            for (Iterator<SelectionKey> it = keys.iterator(); it.hasNext(); ) {

                SelectionKey sk = it.next();
                it.remove();

                if (sk.isValid() && sk.isAcceptable()) {
                    handleAccept();
                } else if (sk.isValid() && sk.isReadable()) {
                    handleRead(sk);
                } else if (sk.isValid() && sk.isWritable()) {
                    handleWrite(sk);
                }
            }
        }
    }

    private void handleAccept() throws IOException {
        SocketChannel socketChannel = serverSocketChannel.accept();

        if (socketChannel != null) {
            socketChannel.configureBlocking(false);
            socketChannel.register(selector, SelectionKey.OP_READ);

            pendingData.put(socketChannel, new LinkedList<ByteBuffer>());
        }
    }

    private void handleRead(SelectionKey sk) throws IOException {
        SocketChannel socketChannel = (SocketChannel) sk.channel();
        ByteBuffer byteBuffer = ByteBuffer.allocate(120);

        int read = socketChannel.read(byteBuffer);
        if (read == -1) {
            pendingData.remove(socketChannel);
            socketChannel.close();
        } else if (read > 0) {
            byteBuffer.flip();
            for (int i = 0; i < byteBuffer.limit(); i++) {
                byteBuffer.put(i, (byte) Character.toUpperCase((char) byteBuffer.get(i)));
            }
            pendingData.get(socketChannel).add(byteBuffer);
            sk.interestOps(SelectionKey.OP_WRITE);
        }
    }

    private void handleWrite(SelectionKey sk) throws IOException {
        SocketChannel socketChannel = (SocketChannel) sk.channel();
        Queue<ByteBuffer> queue = pendingData.get(socketChannel);

        while (!queue.isEmpty()) {
            ByteBuffer byteBuffer = queue.peek();
            int write = socketChannel.write(byteBuffer);
            if (write == -1) {
                pendingData.remove(socketChannel);
                socketChannel.close();
                return;
            } else if (byteBuffer.hasRemaining()) {
                return;
            }

            queue.remove();
        }
        sk.interestOps(SelectionKey.OP_READ);
    }
}