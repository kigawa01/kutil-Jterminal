package net.kigawa.kutil.terminal;

import jline.console.ConsoleReader;
import net.kigawa.kutil.kutil.interfaces.LoggerInterface;
import net.kigawa.kutil.kutil.interfaces.Module;
import net.kigawa.kutil.kutil.thread.ThreadExecutors;
import net.kigawa.kutil.log.log.Formatter;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.function.Consumer;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;

public class Terminal implements Module {
    public static Terminal terminal;
    public static String PREFIX = "]";

    private final ArrayList<Consumer<String>> consumerList = new ArrayList<>();
    private final boolean jline;
    private final LoggerInterface logger;
    private ConsoleReader consoleReader;
    private BufferedReader reader;
    private BufferedWriter writer;
    private TerminalHandler terminalHandler;

    public Terminal(boolean jline, LoggerInterface logger) {
        this.logger = logger;
        this.jline = jline;
    }

    @SafeVarargs
    public final void addOnRead(Consumer<String>... consumer) {
        Collections.addAll(consumerList, consumer);
    }

    @Override
    public synchronized void enable() {
        logger.info("enable terminal...");
        if (terminal != null) {
            logger.warning("terminal is already exit!");
            return;
        }

        try {
            if (jline) {
                consoleReader = new ConsoleReader(System.in, System.out);
                writer = new BufferedWriter(consoleReader.getOutput());
            } else {
                reader = new BufferedReader(new InputStreamReader(System.in));
                writer = new BufferedWriter(new OutputStreamWriter(System.out));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        terminal = this;

        terminalHandler = new TerminalHandler(Terminal.terminal, new Formatter(), logger);

        java.util.logging.Logger.getLogger("").addHandler(terminalHandler);
        for (Handler handler : java.util.logging.Logger.getLogger("").getHandlers()) {
            if (handler instanceof ConsoleHandler) {
                java.util.logging.Logger.getLogger("").removeHandler(handler);
            }
        }

        ThreadExecutors.execute(this::read);
    }

    @Override
    public synchronized void disable() {
        try {
            writer.close();
            if (jline) consoleReader.close();
            else reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        consoleReader = null;
        reader = null;
        writer = null;
        terminalHandler = null;
        terminal = null;
    }

    private void read() {
        while (true) {
            try {
                String line;
                if (jline) {
                    synchronized (this) {
                        if (consoleReader == null) return;
                    }
                    line = consoleReader.readLine(PREFIX, null);
                } else {
                    synchronized (this) {
                        if (reader == null) return;
                    }
                    line = reader.readLine();
                }

                for (Consumer<String> consumer : consumerList) {
                    try {
                        consumer.accept(line);
                    } catch (Exception e) {
                        logger.warning(e);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public ConsoleReader getConsoleReader() {
        return null;
    }

    public BufferedReader getReader() {
        return reader;
    }

    public BufferedWriter getWriter() {
        return writer;
    }

    public synchronized void write(String str) {
        try {
            synchronized (this) {
                if (writer == null) {
                    System.out.println(str);
                    System.out.println("terminal is not enable!");
                    return;
                }
            }

            writer.write(str);
            writer.flush();
            consoleReader.drawLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
