package ken;

import com.intellij.AppTopics;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.components.BaseComponent;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileDocumentManagerListener;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.messages.MessageBusConnection;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * This class handles the OnFileSave component. It implements the ApplicationComponent interface and
 * overrides (implements) the beforeDocumentSaving method of the interface FileDocumentManagerListener.
 * @author Ken Studdy
 * @date September 23, 2019
 * @version 1.5
 */
public class OnFileSaveComponent implements BaseComponent {
    private String fileName;
    private String fileExtension;
    private String src;
    private String dest;
    private String output;
    private String logTime;

    /**
     * Return the component name.
     * @return component name
     */
    @NotNull
    public String getComponentName() {
        return "OnFileSave Component";
    }

    /**
     * Initialize the component.
     */
    public void initComponent() {
        MessageBus bus = ApplicationManager.getApplication().getMessageBus();
        MessageBusConnection connection = bus.connect();

        connection.subscribe(AppTopics.FILE_DOCUMENT_SYNC,
                new FileDocumentManagerListener() {

                    /**
                     * Handle the saving of the documents. This overrides (implements) the beforeDocumentSaving method in the FileDocumentManagerListener interface.
                     * @param document The current document
                     */
                    @Override
                    public void beforeDocumentSaving(Document document) {
                        //Loop through all of the open unsaved Documents in the IDE.
                        for (int documentCounter = 0; documentCounter < FileDocumentManager.getInstance().getUnsavedDocuments().length; documentCounter++) {
                            //Get the current element of the Documents array as a file.
                            VirtualFile file = FileDocumentManager.getInstance().getFile(FileDocumentManager.getInstance().getUnsavedDocuments()[documentCounter]);

                            //If the file does not exist, we do not want to save it, so return early.
                            if (file == null) {
                                return;
                            }

                            //This is actually the full name and path of the file, not just the path.
                            fileName = file.getPath();

                            //The file extension of a file called "main.java" would be "java".
                            fileExtension = file.getExtension();

                            //Here is the date and time that the file is saved at.
                            logTime = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(Calendar.getInstance().getTime());

                            //Here we remove the extension from the file name.
                            src = fileName.replace("." + fileExtension, "");

                            //On Windows, we cannot have : in the folder name, and we might as well remove it for other operating systems too for cross-platform compatibility.
                            src = src.replace(":", "");

                            //The destination folder should be in the user's home directory, this works on most operating systems and prevents permission issues.
                            dest = System.getProperty("user.home") + File.separator + ".SaveBackup";

                            //If we are on Windows, we need to append another file separator to the end of our destination folder.
                            if (System.getProperty("os.name").startsWith("Windows")) {
                                dest += File.separator;
                            }

                            //The output file is based on the destination folder with the source folder and file name appended to the destination folder in addition to a time stamp of when the file was saved.
                            output = dest + src + "-" + logTime + "." + fileExtension;
                            try {
                                File newFile = new File(output);

                                //Create all the folders for the output directory if they don't exist, this also works with Linux by incrementally creating the folders one at a time.
                                if (!newFile.getParentFile().exists()) {
                                    newFile.getParentFile().mkdirs();
                                }

                                BufferedWriter writer = new BufferedWriter(new FileWriter(newFile));

                                //Write to the file based on the entire text of the document instead of looping through the lines.
                                writer.write(document.getText());
                                //We are done writing, so close the writer.
                                writer.close();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
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
                });
    }

    /**
     * Handle the destruction of the component.
     */
    public void disposeComponent() {

    }
}
