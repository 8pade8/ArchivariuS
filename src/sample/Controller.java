package sample;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;
import java.sql.SQLOutput;
import java.util.ArrayList;

public class Controller {

    ArrayList<String> listFileName;

    String packFullDirectory;
    String archiveFullDirectory;
    String archiveFileDirectory;
    String targetDirectory;
    byte startOpenTag[] = "<#fileName".getBytes();
    byte endOpenTag[] = "#>".getBytes();
    byte closeTag[] = "</#fileName#>".getBytes();

    @FXML
    TextField textFieldPackFullDirectory, textFieldArchiveDirectory, textFieldArchiveFileDirectory, textFieldTargetDirectory;
    @FXML
    Label labelArchiveProgress;
    @FXML
    ProgressBar archiveProgressBar, unPackProgressBar;

    @FXML
        public void pack(ActionEvent actionEvent) {


        if (textFieldPackFullDirectory.getText().equals("")||textFieldArchiveDirectory.getText().equals("")){
            labelArchiveProgress.setText("Файл не найден");
            return;
        }
        Thread thread = new Thread(() -> {
            packFullDirectory = textFieldPackFullDirectory.getText();
            archiveFullDirectory = textFieldArchiveDirectory.getText();
            listFileName = new ArrayList<>();
            File file = new File(packFullDirectory);
            getDirectoryNodes(packFullDirectory, listFileName, file);
            double i=0;
            for (String s : listFileName
            ) {
                i++;
                s = s.replace(packFullDirectory + "\\", "");
                try{
                    writeCryptoFile(archiveFullDirectory,
                            packFullDirectory+"\\"+s,s);}
                catch (IOException e){
                }
                archiveProgressBar.setProgress(i/(double)listFileName.size());

            }
        });
            thread.start();

    }


    private void getDirectoryNodes(String path, ArrayList<String> list, File file) {
        for (String f: file.list()
             ) {
            File nodeFile = new File(path+"\\"+f);
            if (nodeFile.isDirectory()) {
                getDirectoryNodes(nodeFile.getAbsolutePath(),list,nodeFile);
            } else {
                list.add(nodeFile.getAbsolutePath());
            }
        }
    }

    private void writeCryptoFile(String archiveFilePath, String fileNamePath, String fileName) throws IOException {

        FileOutputStream fileOutputStream = new FileOutputStream(archiveFilePath,true);
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
        FileInputStream fileInputStream = new FileInputStream(fileNamePath);
        BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);


        bufferedOutputStream.write(enCrypt(startOpenTag));
        bufferedOutputStream.flush();



        bufferedOutputStream.write(enCrypt(fileName.getBytes()));
        bufferedOutputStream.flush();



        bufferedOutputStream.write(enCrypt(endOpenTag));
        bufferedOutputStream.flush();


        int bufferSize=5000;
        byte buffer[];
        if (bufferedInputStream.available() < bufferSize) {
            buffer = new byte[bufferedInputStream.available()];
        }
        else {
            buffer = new byte[bufferSize];}

        while (bufferedInputStream.available()>0){
            bufferedInputStream.read(buffer);

            bufferedOutputStream.write(enCrypt(buffer));
            bufferedOutputStream.flush();


        }
        bufferedOutputStream.write(enCrypt(closeTag));
        bufferedOutputStream.close();
        bufferedInputStream.close();
        bufferedOutputStream.close();

    }


    public void choosePackFullDirectory(ActionEvent actionEvent) {
        labelArchiveProgress.setText("");
        archiveProgressBar.setProgress(0);
        DirectoryChooser fileChooser = new DirectoryChooser();
        textFieldPackFullDirectory.setText("");
        fileChooser.setTitle("Выбор директории");
        File file = fileChooser.showDialog(new Stage());

        if (file != null) {
            textFieldPackFullDirectory.setText(file.getAbsolutePath());
        }
    }

    public void chooseDirectoryArchive(ActionEvent actionEvent) {
        labelArchiveProgress.setText("");
        archiveProgressBar.setProgress(0);
        if (textFieldPackFullDirectory.getText().equals("")){
            return;
        }
        DirectoryChooser fileChooser = new DirectoryChooser();
        textFieldArchiveDirectory.setText("");
        fileChooser.setTitle("Выбор директории");
        File file = fileChooser.showDialog(new Stage());
        File file2 = new File(textFieldPackFullDirectory.getText());
        if (file != null) {
            textFieldArchiveDirectory.setText(file.getAbsolutePath()+"\\"+file2.getName()+".arch");
        }



    }

    public void chooseArchiveFileDirectory (ActionEvent actionEvent) {

        FileChooser fileChooser = new FileChooser();
        textFieldArchiveFileDirectory.setText("");
        fileChooser.setTitle("Выбор файла");
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("ARCH", "*.arch"));
        File file = fileChooser.showOpenDialog(new Stage());
        if (file != null) {
            textFieldArchiveFileDirectory.setText(file.getAbsolutePath());
        }
    }

    public void chooseTargetDirectory (ActionEvent actionEvent) {

        DirectoryChooser fileChooser = new DirectoryChooser();
        textFieldTargetDirectory.setText("");
        fileChooser.setTitle("Выбор папки");
        File file = fileChooser.showDialog(new Stage());
        if (file != null) {
            textFieldTargetDirectory.setText(file.getAbsolutePath());
        }
    }

    public void unPack(ActionEvent actionEvent) {
        if (textFieldTargetDirectory.getText().equals("") || textFieldArchiveFileDirectory.getText().equals("")) {
            return;
        }
        archiveFileDirectory = textFieldArchiveFileDirectory.getText();
        targetDirectory = textFieldTargetDirectory.getText();

        FileDecoder thread = new FileDecoder(targetDirectory, archiveFileDirectory,unPackProgressBar);
        thread.start();


    }

    private byte[] enCrypt(byte[] array2) {
        byte[] array = new byte[array2.length];
        for (int i=0;i<array2.length;i++) {
            byte b = array2[i];
            if (b == 127) {
                array[i]=-128;
            }
            else {array[i]= ++b;}
        }
        return array;
    }

    public static String arrayToString(byte[] array) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < array.length; i++) {
            sb.append((int) array[i]).append("-");
        }
        return sb.toString();
    }
}
