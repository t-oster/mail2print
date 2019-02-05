# mail2print
A Java Command Line application, which waits for E-Mails and prints them on a printer.

Examples:
```
java -jar Mail2Print.jar -i -h your.imap.server -u username -P password -p YourPrinter -o /tmp/
```
will connect to your IMAP Server and wait for incoming mails. If on any new mail there is an attachment (pdf,docx,jpg,png), it will be printed on YourPrinter and saved to /tmp.

When using JPG/PNG images, you can even specify a size in the subject such as "10x20cm".
For docx support, you will need libreoffice/openoffice to be installed.


Usage:
```
usage: mail2print
 -c,--convert-office-files   Use Libre/Open Office to convert Office
                             Documents
 -h,--host <arg>             IMAP server
 -i,--idle-mode              Use IMAP-IDLE to wait for new messages
 -o,--output-folder <arg>    Folder where to save attachments.
 -p,--printer <arg>          Printer to use. If not specified, files won't
                             be printed
 -P,--password <arg>         Password for the IMAP account
 -u,--username <arg>         Username for the IMAP account
