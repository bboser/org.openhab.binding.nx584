package org.openhab.binding.nx584.internal.panel;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Date;

import org.openhab.binding.nx584.internal.panel.util.BooleanLock;
import org.openhab.binding.nx584.internal.panel.util.FIFO;
import org.openhab.binding.nx584.internal.panel.util.ListenerApplicator;
import org.openhab.binding.nx584.internal.panel.util.ListenerQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gnu.io.NRSerialPort;

public class NX584 {

    private final NRSerialPort serialPort;
    private final Logger logger = LoggerFactory.getLogger(NX584.class);
    private Transmitter transmitter;
    private Receiver receiver;
    private final BooleanLock ackLock = new BooleanLock(true); // handshake

    public NX584(String serialPortName, int baudRate) {
        serialPort = new NRSerialPort(serialPortName, baudRate);
    }

    // connect serial port to panel & start threads
    public void connect() {
        if (serialPort.isConnected()) {
            logger.info("NX584.connect: already connected");
            return;
        }
        try {
            serialPort.connect();
            receiver = new Receiver();
            transmitter = new Transmitter();
            receiver.start();
            transmitter.start();
            // TODO:
            // delete next two statements
            serialPort.getInputStream();
            serialPort.getOutputStream();
        } catch (Exception ex) {
            logger.error("cannot connect to security panel", ex);
        }
    }

    // release serial port & stop threads
    public void disconnect() {
        if (serialPort.isConnected()) {
            serialPort.disconnect();
        }
        transmitter.stop();
        receiver.stop();
    }

    /**
     * Send message to panel. Asynchronous.
     *
     * @param cmd NX584 message, e.g. sendCommand(0x25, 0) for Zone 1 Name Request
     */
    public void sendCommand(byte... msg) {
        transmitter.transmit(msg);
    }

    public void setClock(Date date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        transmitter.transmit((byte) 0x3b, (byte) (c.get(Calendar.YEAR) - 2000), (byte) (c.get(Calendar.MONTH) + 1),
                (byte) (c.get(Calendar.DAY_OF_MONTH)), (byte) (c.get(Calendar.HOUR_OF_DAY)),
                (byte) (c.get(Calendar.MINUTE)), (byte) (c.get(Calendar.DAY_OF_WEEK)));
    }

    public void addSecurityPanelListener(SecurityPanelListener listener) {
        receiver.addSecurityPanelListener(listener);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Transmitter
    private class Transmitter implements Runnable {

        private Thread transmitThread;
        private final FIFO<byte[]> transmitMessages = new FIFO<>(100);

        public Transmitter() {
        }

        public void start() {
            try {
                transmitThread = new Thread(this, "NX584 transmitter");
                transmitThread.start();
                logger.info("transmitter started");
            } catch (Throwable t) {
                logger.error("cannot start transmitter thread", t);
            }
        }

        public void stop() {
            // kick thread out of waiting for FIFO
            transmitThread.interrupt();
        }

        /**
         * Send message to panel. Asynchronous, i.e. returns immediately.
         * Messages are sent only when panel is ready (i.e. sent an
         * acknowledgment to last message), but no checks are implemented that
         * the message was correctly received and executed.
         *
         * @param data Data to be sent to panel. First byte is message type.
         *            Fletcher sum is computed and appended by transmit.
         */
        public void transmit(byte... msg) {
            try {
                transmitMessages.add(msg);
            } catch (InterruptedException ie) {
                logger.error("transmit buffer overflow", ie);
            }
        }

        /**
         * Transmit this message before any others from buffer.
         * Used by receiver to send ACKs.
         */
        public void transmitFirst(byte... msg) {
            try {
                transmitMessages.prepend(msg);
            } catch (InterruptedException ie) {
                logger.error("transmit_first buffer overflow", ie);
            }
        }

        @Override
        public void run() {
            try {
                OutputStream out = serialPort.getOutputStream();
                while (true) {
                    ackLock.waitToSetFalse(5000);
                    byte msg[] = transmitMessages.remove();
                    out.write(0x7e); // start character
                    out.write(msg.length); // length byte
                    if (msg[0] != 0x1d) {
                        // logger.debug("transmitter sending command to panel: " + bytes2string(msg));
                    }
                    // data (including msg byte) ... beware of the 0x7e "stuffing" issue
                    for (int i = 0; i < msg.length; i++) {
                        byte b = msg[i];
                        if (b == 0x7e) {
                            out.write(0x7d);
                            b = 0x5e;
                        } else if (b == 0x7d) {
                            out.write(0x7d);
                            b = 0x5d;
                        }
                        out.write(b);
                    }
                    // fletcher sum
                    byte f[] = fletcher(msg);
                    out.write(f, 0, 2);
                    out.flush();
                    // no ack for acknowledge
                    if (msg[0] == 0x1d) {
                        ackLock.setValue(true);
                    }
                }
            } catch (InterruptedException | IOException ex) {
                logger.info("transmitter shutdown");
            } catch (Throwable t) {
                logger.error("transmitter terminated unexpectedly");
            }
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Receiver
    private class Receiver implements Runnable {

        private final ListenerQueue<SecurityPanelListener> listenerQueue = new ListenerQueue<>();

        public Receiver() {
        }

        public void start() {
            try {
                Thread receiveThread = new Thread(this, "NX584 receiver");
                receiveThread.start();
                logger.info("receiver started");
            } catch (Throwable t) {
                logger.error("cannot start receiver thread", t);
            }
        }

        public void stop() {
            // no action needed ... closing serialPort stops receiver
        }

        public void addSecurityPanelListener(SecurityPanelListener listener) {
            listenerQueue.addListener(listener);
        }

        @Override
        public void run() {
            try {
                InputStream is = serialPort.getInputStream();
                while (true) {
                    // look for start byte
                    while ((is.read()) != 0x7e) {
                        ;
                    }
                    // get length
                    int len = is.read();
                    // data, first byte is message type
                    final byte data[] = new byte[len];
                    for (int i = 0; i < len; i++) {
                        int x = is.read();
                        if (x == 0x7d) {
                            x = is.read() == 0x5e ? 0x7e : 0x7d;
                        }
                        data[i] = (byte) x;
                    }
                    // logger.debug("receiver got message " + bytes2string(data));
                    // check fletcher sum
                    byte fletcher_sum[] = fletcher(data);
                    byte sum1 = (byte) is.read();
                    byte sum2 = (byte) is.read();
                    boolean fletcher_ok = sum1 == fletcher_sum[0] && sum2 == fletcher_sum[1];
                    if ((!fletcher_ok) && ((data[0] & 63) != 0x05)) { // zoneSnapshot has errors???
                        logger.warn(
                                String.format("receiver: fletcher error for msg len = %d: ", len) + bytes2string(data));
                        logger.warn(String.format(
                                "receiver: fletcher sum1 for msg type 0x%02x - expected 0x%02x got 0x%02x",
                                data[0] & 63, fletcher_sum[0], sum1));
                        logger.warn(String.format(
                                "receiver: fletcher sum2 for msg type 0x%02x - expected 0x%02x got 0x%02x",
                                data[0] & 63, fletcher_sum[1], sum2));
                    }
                    // acknowledge & handshake
                    if ((data[0] & 128) != 0) {
                        // byte ack_msg = 0x1d;
                        byte ack_msg = (byte) (sum1 == fletcher_sum[0] && sum2 == fletcher_sum[1] ? 0x1d : 0x1e);
                        ack_msg = 0x1d;
                        transmitter.transmitFirst(ack_msg);
                    }
                    ackLock.setValue(true); // notify transmitter that receiver is ready
                    // handle the message
                    if (fletcher_ok) {
                        listenerQueue.apply(new ListenerApplicator<SecurityPanelListener>() {
                            @Override
                            public void apply(final SecurityPanelListener l) {
                                l.nx584message(data[0] & 63, data);
                            }
                        });
                    }
                }
            } catch (IOException io) {
                logger.info("receiver shutdown");
            } catch (Throwable t) {
                logger.error("receiver terminated unexpectedly", t);
            }

        }

    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Utility

    private byte[] fletcher(byte data[]) {
        // logger.debug("fletcher of " + bytes2string(data));
        int len = data.length;
        int sum1 = len, sum2 = len;
        for (int i = 0; i < len; i++) {
            int d = data[i] & 0xff;
            // System.out.printf("d = %d = 0x%02x\n", d, d);
            // System.out.printf("0xff - 0x%02x < 0x%02x ? %b\n", sum1, d, 255 - sum1 < d);
            if (0xff - sum1 < d) {
                sum1 = (sum1 + 1) & 0xff;
            }
            sum1 = (sum1 + d) & 0xff;
            if (sum1 == 0xff) {
                sum1 = 0;
            }
            if (0xff - sum2 < sum1) {
                sum2 = (sum2 + 1) & 0xff;
            }
            sum2 = (sum2 + sum1) & 0xff;
            if (sum2 == 0xff) {
                sum2 = 0;
            }
            // logger.debug(String.format("0x%02x 0x%02x 0x%02x", d, sum1, sum2));
        }
        return new byte[] { (byte) sum1, (byte) sum2 };
    }

    public static String bytes2string(byte msg[]) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < msg.length; i++) {
            b.append(String.format(" 0x%02x", msg[i] & 0xff));
        }
        return b.toString();
    }

}
