package ken;

import com.intellij.AppTopics;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileDocumentManagerAdapter;
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
 * overrides the beforeDocumentSaving method of the abstract class FileDocumentManagerAdapter.
 * @author Ken Studdy
 * @date July 15, 2018
 * @version 1.0
 */
public class OnFileSaveComponent implements ApplicationComponent {
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
                new FileDocumentManagerAdapter() {

                    /**
                     * Handle the saving of the document. This overrides the beforeDocumentSaving method in the FileDocumentManagerAdapter abstract class.
                     * @param document The current document
                     */
                    @Override
                    public void beforeDocumentSaving(Document document) {
                        VirtualFile file = FileDocumentManager.getInstance().getFile(document);

                        //This is actually the full name and path of the file, not just the path.
                        fileName = file.getPath();

                        //The file extension of a file called "main.java" would be "java".
                        fileExtension = file.getExtension();

                        //Here is the date and time that the file is saved at.
                        logTime = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(Calendar.getInstance().getTime());

                        //Here we remove the extension from the file name.
                        src = fileName.replace("." + fileExtension, "");

                        //The destination folder should be in the user's home directory, this works on most operating systems and prevents permission issues.
                        dest = System.getProperty("user.home") + "/.SaveBackup";

                        //The output file is based on the destination folder with the source folder and file name appended to the destination folder in addition to a time stamp of when the file was saved.
                        output = dest + src + "-" + logTime + "." + fileExtension;
                        try {
                            File newFile = new File(output);

                            //Create all the folders for the output directory, this also works with Linux by incrementally creating the folders one at a time.
                            newFile.getParentFile().mkdirs();

                            BufferedWriter writer = new BufferedWriter(new FileWriter(newFile));

                            //Write to the file based on the entire text of the document instead of looping through the lines.
                            writer.write(document.getText());
                            writer.close();
                        }
                        catch(Exception e) {
                            e.printStackTrace();
                        }
                    }
        });
    }

    /**
     * Handle the destruction of the component.
     */
    public void disposeComponent() {

    }
}