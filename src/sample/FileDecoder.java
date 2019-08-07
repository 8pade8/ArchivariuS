package sample;
import com.sun.deploy.util.ArrayUtil;
import javafx.scene.control.ProgressBar;

import java.io.*;
import java.nio.charset.Charset;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileDecoder extends Thread {

    String unPackDirectory;
    String archiveFileFullDirectory;
    ProgressBar unPackProgressBar;
    byte startOpenTag[] = "<#fileName".getBytes();
    byte endOpenTag[] = "#>".getBytes();
    byte closeTag[] = "</#fileName#>".getBytes();


       public FileDecoder(String unPackDirectory, String archiveFileFullDirectory, ProgressBar unPackProgressBar) {
        this.unPackProgressBar = unPackProgressBar;
        this.unPackDirectory = unPackDirectory;
        this.archiveFileFullDirectory = archiveFileFullDirectory;

    }

    @Override
    public void run() {

        try {
            FileInputStream fileInputStream = new FileInputStream(archiveFileFullDirectory);
            BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
            long fileSize = bufferedInputStream.available();
            long progress=0;
            boolean endWrite = false;
            byte buffer[];
            int bufferSize = 5000;
            while (bufferedInputStream.available() > 0) {
                if (bufferedInputStream.available()< bufferSize){
                    buffer = new byte[bufferedInputStream.available()];
                } else {
                    buffer = new byte[bufferSize];
                }
                bufferedInputStream.read(buffer);
                progress+=buffer.length;
                endWrite =  writeFile(buffer);
                double doubleProgress = (double)progress/(double)fileSize;
                unPackProgressBar.setProgress(doubleProgress);
            }
            while (!endWrite) {
                writeFile(new byte[0]);
            }
            fileInputStream.close();
            bufferedOutputStream.close(); //sadf

        } catch (IOException e) {
            System.out.println("File not found");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private boolean hasStartTag(byte[] s){
        return byteParse(s, "<".getBytes())[0]>-1;
    }

    private boolean hasOpenTag(byte[] array){
        return byteParse(array, startOpenTag)[0]>-1;
    }

    private boolean hasFinishTag(byte[] array){
       return byteParse(array,closeTag)[0]>-1;
    }

    private String parseFileName(byte[] s) {
        int startFileNameIndex = byteParse(s, startOpenTag)[1]+1;
        int finishFileNameIndex = byteParse(s, endOpenTag)[0];
        byte[] b = Arrays.copyOfRange(s, startFileNameIndex, finishFileNameIndex);
        String res = new String(b);
        makeDir(res);
        return res;
    }

    private void makeDir(String res) {
           String arr[] = res.split("\\\\");
           if (arr.length>1){
               StringBuilder dirName =new StringBuilder(unPackDirectory);
               for (int i =0; i<arr.length-1;i++){
                   for (int g=0; g <= i; g++) {
                       dirName.append("\\"+arr[g]);
                   }
                   File file = new File(dirName.toString());
                   file.mkdir();
               }
           }
    }

    String fileName = "";
       boolean isWritingStarted=false;
       BufferedOutputStream bufferedOutputStream;
    byte[] wbuffer = new byte[0];

    private boolean writeFile(byte[] array) throws Exception {
        array = coopAr(wbuffer, unCoding(array));
        wbuffer = new byte[0];
        if (isWritingStarted) { //запись начата?
            if (hasOpenTag(array)) { //ищим тег
                if (hasFinishTag(array)) { //есть ли закрывающий тег?
                    int[] startClose = (byteParse(array,closeTag));
                    if (startClose[1] != array.length-1){
                       wbuffer = Arrays.copyOfRange(array,startClose[1]+1,array.length); //отгружаем все что после тега в буфер
                    }
                    array = Arrays.copyOfRange(array,0,startClose[0]);
                    bufferedOutputStream.write(array); //пишем все что до тега
                    bufferedOutputStream.close();
                    isWritingStarted=false;
                } else {
                    wbuffer = array;
                    isWritingStarted=true;
                }
            } else {
                bufferedOutputStream.write(array);
                isWritingStarted=true;
            }
        } else {
            if (hasOpenTag(array)) { //ищем открывающий тег
                fileName = parseFileName(array); //парсим имя файла
                array = Arrays.copyOfRange(array,byteParse(array,endOpenTag)[1]+1,array.length); //обрезаем начало (поидеии сам тег)
                File file = new File(unPackDirectory + "\\" + fileName);
                bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(unPackDirectory+"\\"+fileName)); // открываем поток записи
                if (hasStartTag(array)) { //есть ли тег в оставшемся куске
                    if (hasFinishTag(array)) { //есть ли закрывающий тег
                        int[] startClose = (byteParse(array,closeTag));
                        if (startClose[1] != array.length-1){
                            wbuffer = Arrays.copyOfRange(array,startClose[1]+1,array.length); //отгружаем все что после тега в буфер
                        }
                        array = Arrays.copyOfRange(array,0,startClose[0]);
                        bufferedOutputStream.write(array); //пишем все что до тега
                        bufferedOutputStream.close();
                        isWritingStarted=false;
                    } else {
                        wbuffer = array;
                        isWritingStarted=true;
                    }
                } else {
                    bufferedOutputStream.write(array);
                    isWritingStarted=true;
                }
            } else{
                System.out.println("Открывающий тег не найден");
            }
        }
        return wbuffer.length==0;
    }

    public static byte[] unCoding(byte[] str) {

        for (int i = 0; i <str.length ; i++) {
            if (str[i] == -128) {
                str[i]=127;
            }
            else {str[i]=--str[i];}
        }

        return str;
    }

    private int[] byteParse(byte[] array, byte[] regex) {
        int[] startEndIndex = new int[]{-1,-1};
        for (int i = 0; i < array.length; i++) {
            if (array[i] == regex[0]) {
                boolean b = true;
                for (int j = 0; j <regex.length; j++) {
                    if (!((i+j)<array.length) || array[i + j] != regex[j]) {
                        b = false;
                        break;
                    }
                }
                if (b) {
                    startEndIndex[0]=i;
                    startEndIndex[1]=i+regex.length-1;
                    break;
                }
            }
        }
        return startEndIndex;
    }

    private byte[] coopAr(byte[] array1, byte[] array2) {
        byte[] res = new byte[array1.length + array2.length];
        int i = 0;
        for (int j = 0; j < array1.length; j++,i++) {
            res[i] = array1[j];
        }
        for (int j = 0; j < array2.length; j++,i++) {
            res[i] = array2[j];
        }
        return res;
    }
}
