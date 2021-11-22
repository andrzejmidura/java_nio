import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

class Serwer {
    // Z każdym klientem powiążemy kolejkę oczekujących na wysłanie buforów.
    private final Map<SocketChannel, Queue<ByteBuffer>> pendingData = new HashMap<>();

    private ServerSocketChannel ssc;
    private Selector selector;

    public void start() throws IOException, InterruptedException {
        selector = Selector.open();

        ssc = ServerSocketChannel.open();
        ssc.bind(new InetSocketAddress(8090));
        ssc.configureBlocking(false);

        // Chcemy być informowani o gotowości do akceptacji nowego połączenia.
        ssc.register(selector, SelectionKey.OP_ACCEPT);

        while (true) {
            TimeUnit.MILLISECONDS.sleep(10);
            selector.select();
            Set<SelectionKey> keys = selector.selectedKeys();

            for (Iterator<SelectionKey> it = keys.iterator(); it.hasNext(); ) {

                SelectionKey sk = it.next();
                // Należy pamiętać o usuwaniu przetworzonych kluczy!
                it.remove();

                if (sk.isValid() && sk.isAcceptable()) {
                    // Nowy klient czeka na akceptację.
                    handleAccept();
                } else if (sk.isValid() && sk.isReadable()) {
                    // Możemy wykonać nieblokującą operację READ na kliencie.
                    handleRead(sk);
                } else if (sk.isValid() && sk.isWritable()) {
                    // Możemy wykonać nieblokującą operację WRITE na kliencie.
                    handleWrite(sk);
                }
            }
        }
    }

    private void handleAccept() throws IOException {
        SocketChannel sc = ssc.accept();

        if (sc != null) {
            sc.configureBlocking(false);
            // Chcemy być informowani o możliwości wykonania nieblokującej
            // operacji READ na kliencie.
            sc.register(selector, SelectionKey.OP_READ);

            pendingData.put(sc, new LinkedList<ByteBuffer>());
        }
    }

    private void handleRead(SelectionKey sk) throws IOException {
        // Pobieramy kanał powiązany z zadanym kluczem.
        SocketChannel sc = (SocketChannel) sk.channel();
        ByteBuffer bb = ByteBuffer.allocate(120);

        // Czytamy z kanału sc do bufora bb. Zmienna read zawiera
        // liczbę przeczytanych znaków.
        int read = sc.read(bb);
        if (read == -1) {
            // -1 -> EOF. Usuwamy klienta z mapy i zamykamy połączenie.
            pendingData.remove(sc);
            sc.close();
        } else if (read > 0) {
            // Aktualizujemy pozycję i limit w buforze, a także podmieniamy jego
            // zawartość na wersję UPPERCASE.
            bb.flip();
            for (int i = 0; i < bb.limit(); i++) {
                bb.put(i, (byte) Character.toUpperCase((char) bb.get(i)));
            }
            // Przetworzony bufor zostaje dodany do kolejki klienta.
            pendingData.get(sc).add(bb);
            // Po odczytaniu danych chcemy wysłać przetworzone wejście z powrotem
            // do klienta, więc od teraz interesuje nas, kiedy można wykonać na nim
            // nieblokującą operację WRITE.
            sk.interestOps(SelectionKey.OP_WRITE);
        }
    }

    private void handleWrite(SelectionKey sk) throws IOException {
        SocketChannel sc = (SocketChannel) sk.channel();
        Queue<ByteBuffer> queue = pendingData.get(sc);

        while (!queue.isEmpty()) {
            ByteBuffer bb = queue.peek();
            // Piszemy do kanału sc z bufora bb. Zmienna write zawiera 
            // liczbę wysłanych znaków.
            int write = sc.write(bb);
            if (write == -1) {
                pendingData.remove(sc);
                sc.close();
                return;
            } else if (bb.hasRemaining()) {
                // Nie udało się wysłać całej zawartości bufora. Oznacza to, że w trakcie
                // wykonywania operacji write przestało być możliwe nieblokujące
                // wysyłanie danych. Opuszczamy metodę - reszta bufora zostanie wysłana
                // przy następnej okazji.
                return;
            }

            // Wysłaliśmy cały bufor. Usuwamy go z kolejki.
            queue.remove();
        }

        // Wysłaliśmy odpowiedź. Wracamy do nasłuchiwania zapytań od klienta.
        sk.interestOps(SelectionKey.OP_READ);
    }
}