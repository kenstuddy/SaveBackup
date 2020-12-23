package ken;

import com.intellij.AppTopics;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileDocumentManagerListener;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.messages.MessageBusConnection;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Properties;

/**
 * This class handles the OnFileSave action. It overrides (implements) the beforeDocumentSaving method of the interface FileDocumentManagerListener.
 * @author Ken Studdy
 * @created July 15, 2018
 * @updated December 22, 2020
 * @version 2.0
 */
public class OnFileSaveComponent {

    /**
     * This constructor calls the init method to initialize the plugin.
     */
    public OnFileSaveComponent() {
        init();
    }
    static Properties properties = new Properties();
    /**
     * Initialize the plugin.
     */
    public void init() {
        try {
            MessageBus bus = ApplicationManager.getApplication().getMessageBus();
            MessageBusConnection connection = bus.connect();
            //Check if our SaveBackup configuration file exists before trying to create it.
            if (!Files.exists(Paths.get(System.getProperty("user.home"), File.separator, "SaveBackup.ini"))) {
                //Here we create a BufferedWriter to create our properties file and store the Properties object.
                try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(System.getProperty("user.home"), File.separator, "SaveBackup.ini"),
                        StandardOpenOption.CREATE)) {
                    //Here, we set the default values for the properties. These properties are user-changeable.
                    properties.put("destinationPath", Paths.get(System.getProperty("user.home"), File.separator, ".SaveBackup").toString());
                    properties.put("fileDateFormat", "yyyy-MM-dd_HH-mm-ss");
                    properties.put("logDateFormat", "dd MMM yyyy, h:mm:ss a");
                    properties.put("infoLogging", "false");
                    properties.put("errorLogging", "false");
                    properties.put("infoLogFile", Paths.get(System.getProperty("user.home"), File.separator, "SaveBackupInfo.txt").toString());
                    properties.put("errorLogFile",  Paths.get(System.getProperty("user.home"), File.separator, "SaveBackupError.txt").toString());
                    properties.store(writer, "");
                } catch (Exception e) {
                    logError("Unable to create " + Paths.get(System.getProperty("user.home"), File.separator, "SaveBackup.ini").toString(), e);
                }
            }
            //Here we create a BufferedReader to read our Properties file and load it into our Properties object.
            try (BufferedReader reader = Files.newBufferedReader(Paths.get(System.getProperty("user.home"), File.separator, "SaveBackup.ini"))) {
                properties.load(reader);
            } catch (Exception e) {
                logError("Unable to load " + Paths.get(System.getProperty("user.home"), File.separator, "SaveBackup.ini").toString(), e);
            }
            connection.subscribe(AppTopics.FILE_DOCUMENT_SYNC,
                    new FileDocumentManagerListener() {

                        String fileName;
                        String fileExtension;
                        String src;
                        String dest = (String) properties.getOrDefault("destinationPath", Paths.get(System.getProperty("user.home"), File.separator, ".SaveBackup").toString());
                        String output;
                        String logTime;
                        String fileDateFormat = (String) properties.getOrDefault("fileDateFormat", "yyyy-MM-dd_HH-mm-ss");

                        /**
                         * Handle the saving of the documents. This overrides (implements) the beforeDocumentSaving method in the FileDocumentManagerListener interface.
                         *
                         * @param document The current document
                         */
                        @Override
                        public void beforeDocumentSaving(@NotNull Document document) {
                            //Get the VirtualFile from the current Document.
                            VirtualFile file = FileDocumentManager.getInstance().getFile(document);

                            //If the file does not exist, we do not want to save it, so return early.
                            if (file == null) {
                                return;
                            }

                            //This is actually the full name and path of the file, not just the path.
                            fileName = file.getPath();

                            //The file extension of a file called "main.java" would be "java".
                            fileExtension = file.getExtension();

                            //Here is the date and time that the file is saved at.
                            logTime = new SimpleDateFormat(fileDateFormat).format(Calendar.getInstance().getTime());

                            //Here we remove the extension from the file name.
                            src = fileName.replace("." + fileExtension, "");

                            //On Windows, we cannot have : in the folder name, and we might as well remove it for other operating systems too for cross-platform compatibility.
                            src = src.replace(":", "");

                            //If we are on Windows, we need to append another file separator to the end of our destination folder.
                            if (System.getProperty("os.name").startsWith("Windows")) {
                                dest += File.separator;
                            }

                            //The output file is based on the destination folder with the source folder and file name appended to the destination folder in addition to a time stamp of when the file was saved.
                            output = dest + src + "-" + logTime + "." + fileExtension;

                            //Log the file name including the path.
                            logInfo("Saving file " + output);

                            //Create the new file.
                            File newFile = new File(output);

                            //Create all the folders for the output directory if they don't exist, this also works with Linux by incrementally creating the folders one at a time.
                            if (!newFile.getParentFile().exists()) {
                                if (!newFile.getParentFile().mkdirs()) {
                                    logError("An error occurred creating the required folder.", new IOException(newFile.getParentFile().getParent()));
                                }
                            }

                            try (BufferedWriter writer = new BufferedWriter(new FileWriter(newFile))) {
                                //Write to the file based on the entire text of the document instead of looping through the lines.
                                writer.write(document.getText());

                            } catch (Exception e) {
                               logError("A problem has occurred with writing the file " + file.getPath(), e);
                            }
                        }

                        //This is the implementation of the methods of interface FileDocumentManagerListener that are not being used for this plugin. The latest version of the of the JetBrains API (known as Open API) actually declares these methods as default (this is a new keyword in Java 8 which allows you to have a default implementation provided in the interface so these methods do not need to be implemented when you implement the interface) but it is still a good practice to implement all methods when implementing an interface.
                        @Override
                        public void beforeAllDocumentsSaving() {

                        }

                        @Override
                        public void beforeFileContentReload(VirtualFile file, @NotNull Document document) {

                        }

                        @Override
                        public void fileContentLoaded(@NotNull VirtualFile file, @NotNull Document document) {

                        }

                        @Override
                        public void fileContentReloaded(@NotNull VirtualFile file, @NotNull Document document) {

                        }

                        @Override
                        public void fileWithNoDocumentChanged(@NotNull VirtualFile file) {

                        }

                        @Override
                        public void unsavedDocumentsDropped() {

                        }
                    }
            );
        } catch (Throwable t) {
            logError("A problem has occurred.", t);
        }
    }

    private static void logError(String error, Throwable t) {
        System.err.println(getTime() + "An error has occurred: " + error);
        t.printStackTrace();
        //We only want to log to the error file if error logging is enabled.
        if (Boolean.parseBoolean((String)properties.getOrDefault("errorLogging", "false"))) {
            try (BufferedWriter writer = Files.newBufferedWriter(Path.of((String)properties.getOrDefault("errorLogFile", Paths.get(System.getProperty("user.home"), File.separator, "SaveBackupError.txt").toString())), StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                writer.write(getTime() + error + System.lineSeparator());
                PrintWriter printWriter = new PrintWriter(writer);
                t.printStackTrace(printWriter);
            } catch (Exception e) {
                System.err.println("An error has occurred writing the error log.");
                e.printStackTrace();
            }
        }
    }

    private static void logInfo(String info) {
        System.out.println(getTime() + info);
        //We only want to log to the error file if info logging is enabled.
        if (Boolean.parseBoolean((String)properties.getOrDefault("infoLogging", "false"))) {
            try (BufferedWriter writer = Files.newBufferedWriter(Path.of((String)properties.getOrDefault("infoLogFile", Paths.get(System.getProperty("user.home"), File.separator, "SaveBackupInfo.txt").toString())), StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                writer.write(getTime() + info + System.lineSeparator());
            } catch (Exception e) {
                System.err.println("An error has occurred writing the info log.");
                e.printStackTrace();
            }
        }
    }

    private static String getTime() {
        DateFormat dateFormat = new SimpleDateFormat((String) properties.getOrDefault("logDateFormat", "dd MMM yyyy, h:mm:ss a"));
        Calendar calendar = Calendar.getInstance();
        return dateFormat.format(calendar.getTime()) + ": ";
    }
}
