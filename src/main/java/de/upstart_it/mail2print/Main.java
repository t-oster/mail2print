package de.upstart_it.mail2print;

import com.sun.mail.imap.IdleManager;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.activation.DataSource;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.event.MessageCountAdapter;
import javax.mail.event.MessageCountEvent;
import javax.mail.internet.MimeMessage;
import javax.mail.search.FlagTerm;
import javax.print.PrintException;
import javax.print.PrintService;
import lombok.Getter;
import lombok.extern.java.Log;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.mail.util.MimeMessageParser;
import org.pf4j.DefaultPluginManager;
import org.pf4j.ExtensionFactory;
import org.pf4j.PluginManager;
import org.pf4j.SingletonExtensionFactory;

/**
 *
 * @author Thomas Oster <thomas.oster@upstart-it.de>
 */
@Log
public class Main {

    private IdleManager idleManager;
    private final Store store;
    private final ExecutorService es;
    private final PrintHelper printHelper = new PrintHelper();
    private final PluginManager pluginManager;
    private final Session session;
    
    private PrintService printer = null;
    @Getter
    private boolean useOfficeConverter = false;
    private boolean idleMode = false;
    private String user = "";
    private String password = "";
    private String hostname = "";
    private File output = null;

    private void parseCommandLine(String[] args) throws ParseException {
        Options options = new Options();

        Option oPrinter = new Option("p", "printer", true, "Printer to use. If not specified, files won't be printed");
        options.addOption(oPrinter);
        
        Option oOutput = new Option("o", "output-folder", true, "Folder where to save attachments.");
        options.addOption(oOutput);

        Option oConvert = new Option("c", "convert-office-files", false, "Use Libre/Open Office to convert Office Documents");
        options.addOption(oConvert);

        Option oIdle = new Option("i", "idle-mode", false, "Use IMAP-IDLE to wait for new messages");
        options.addOption(oIdle);
        
        Option oUser = new Option("u", "username", true, "Username for the IMAP account");
        oUser.setRequired(true);
        options.addOption(oUser);
        
        Option oPassword = new Option("P", "password", true, "Password for the IMAP account");
        oPassword.setRequired(true);
        options.addOption(oPassword);
        
        Option oHost = new Option("h", "host", true, "IMAP server");
        oHost.setRequired(true);
        options.addOption(oHost);
        

        CommandLineParser parser = new GnuParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("mail2print", options);
            throw e;
        }

        if (cmd.hasOption(oPrinter.getLongOpt())) {
            printer = printHelper.getPrinter(cmd.getOptionValue(oPrinter.getLongOpt()));
            if (printer == null) {
                throw new ParseException("Printer "+cmd.getOptionValue(oPrinter.getLongOpt())+" not found");
            }
        }
        if (cmd.hasOption(oOutput.getLongOpt())) {
            output = new File(cmd.getOptionValue(oOutput.getLongOpt()));
            if (!output.isDirectory() || !output.canWrite()) {
                throw new ParseException("Folder "+oOutput.getLongOpt()+" does not exist or is not writeable");
            }
        }
        useOfficeConverter = cmd.hasOption(oConvert.getLongOpt());
        idleMode = cmd.hasOption(oIdle.getLongOpt());
        user = cmd.getOptionValue(oUser.getLongOpt());
        password = cmd.getOptionValue(oPassword.getLongOpt(), "Virtual_PDF_Printer");
        hostname = cmd.getOptionValue(oHost.getLongOpt(), "Virtual_PDF_Printer");
    }

    public Main(String[] args) throws NoSuchProviderException, MessagingException, IOException, ParseException {
        parseCommandLine(args);

        es = Executors.newCachedThreadPool();
        Properties props = System.getProperties();
        props.setProperty("mail.imaps.usesocketchannels", "true");

        // Get a Session object
        session = Session.getInstance(props, null);
        // session.setDebug(true);

        // Get a Store object
        store = session.getStore("imaps");
        
        pluginManager = new DefaultPluginManager() {
            @Override
            protected ExtensionFactory createExtensionFactory() {
                return new SingletonExtensionFactory();
            }
        };
        log.info("loading Plugins...");
        pluginManager.loadPlugins();
        pluginManager.startPlugins();
        for (ConverterPlugin plugin : pluginManager.getExtensions(ConverterPlugin.class)) {
            log.log(Level.INFO, "Plugin {0} loaded", plugin.getName());
        }
    }
    
    private void shutdown() throws MessagingException {
        for (ConverterPlugin plugin : pluginManager.getExtensions(ConverterPlugin.class)) {
            plugin.shutdown();
        }
        pluginManager.stopPlugins();
        if (idleMode && idleManager != null && idleManager.isRunning()) {
            idleManager.stop();
        }
        store.close();
        es.shutdownNow();
    }

    private List<DataSource> getAttachments(MimeMessage msg) throws Exception {
        MimeMessageParser parser = new MimeMessageParser(msg);
        parser.parse();
        return parser.getAttachmentList();
    }

    private void print(String subject, DataSource ds) throws IOException, PrintException {
        String contentType = ds.getContentType().toLowerCase();
        String filename = ds.getName().toLowerCase();
        byte[] data = null;
        if (filename.endsWith(".pdf") || contentType.contains("application/pdf")) {
            log.log(Level.FINE, "Printing {0} with type {1}", new Object[]{ds.getName(), ds.getContentType()});
            data = ds.getInputStream().readAllBytes();
        } else {
            for (ConverterPlugin plugin : pluginManager.getExtensions(ConverterPlugin.class)) {
                if (plugin.canConvertFile(contentType, filename, subject)) {
                    log.log(Level.INFO, "Using {0} to convert {1}", new Object[]{plugin.getName(), filename});
                    data = plugin.convertToPdf(ds.getInputStream(), contentType, filename, subject);
                    break;
                }
            }
            if (data == null) {
                log.log(Level.INFO, "Skipping unsupported {0} with type {1}", new Object[]{ds.getName(), ds.getContentType()});
                return;
            }
        }
        printHelper.printPDF(printer, data, null, null);
    }

    private void process(Message msg) throws Exception {
        log.log(Level.FINE, "processing {0}", msg.getSubject());
        for (DataSource e : getAttachments((MimeMessage) msg)) {
            if (output != null) {
                int number = 0;
                File target = new File(output, e.getName());
                while (target.exists()) {
                    target = new File(output, (++number)+e.getName());
                }
                FileUtils.writeByteArrayToFile(target, e.getInputStream().readAllBytes());
                e.getInputStream().reset();
            }
            if (printer != null) {
                print(msg.getSubject(), e);
            }
        }
    }

    private void processUnreadMessages(Folder folder) throws MessagingException, Exception {
        // Fetch unseen messages from inbox folder
        Message[] messages = folder.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));
        log.log(Level.INFO, "Processing {0} unread messages...", messages.length);
        // Sort messages from recent to oldest
        Arrays.sort(messages, (m1, m2) -> {
            try {
                return m2.getSentDate().compareTo(m1.getSentDate());
            } catch (MessagingException e) {
                throw new RuntimeException(e);
            }
        });
        for (Message msg : messages) {
            process(msg);
        }
    }

    private void run() throws MessagingException, Exception {
        // Connect
        log.log(Level.INFO, "connecting to {0}", hostname);
        store.connect(hostname, user, password);

        Folder folder = store.getFolder("INBOX");
        folder.open(Folder.READ_WRITE);
        processUnreadMessages(folder);
        if (!idleMode) {
            folder.close();
            shutdown();
            return;
        }
        log.info("Waiting for new messages...");
        idleManager = new IdleManager(session, es);
        folder.addMessageCountListener(new MessageCountAdapter() {
            @Override
            public void messagesAdded(MessageCountEvent ev) {
                Folder folder = (Folder) ev.getSource();
                Message[] msgs = ev.getMessages();
                log.log(Level.INFO, "Folder: {0} got {1} new messages", new Object[]{folder, msgs.length});
                for (Message msg : msgs) {
                    try {
                        process(msg);
                    } catch (Exception ex) {
                        Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                try {
                    // process new messages
                    log.info("Waiting for new messages...");
                    idleManager.watch(folder); // keep watching for new messages
                } catch (MessagingException mex) {
                    // handle exception related to the Folder
                }
            }
        });
        new Thread() {
            @Override
            public void run() {
                while (!Thread.interrupted()) {
                    try {
                        Thread.sleep(300000);//5min
                        log.fine("Checking if connection is alive...");
                        folder.getNewMessageCount();
                    } catch (InterruptedException e) {
                        // Ignore, just aborting the thread...
                    } catch (MessagingException ex) {
                        Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }

        }.start();
        idleManager.watch(folder);
    }

    public static void main(String[] args) {
        Main instance = null;
        try {
            instance = new Main(args);
            instance.run();
        } catch (MessagingException ex) {
            log.log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            log.log(Level.SEVERE, null, ex);
        } catch (ParseException ex) {
            //output is already done, just exit
            System.exit(1);
        } catch (Exception ex) {
            log.log(Level.SEVERE, null, ex);
        } finally {
            if (instance != null) {
                try {
                    instance.shutdown();
                } catch (MessagingException ex) {
                    log.log(Level.SEVERE, null, ex);
                }
            }
        }
    }
}
