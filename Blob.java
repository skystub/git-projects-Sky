import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.io.FileOutputStream;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.BufferedOutputStream;
import java.util.zip.Deflater;
import java.io.ByteArrayOutputStream;


public class Blob {
    public static boolean compressionEnabled;
    public static void main(String[] args) throws FileNotFoundException, NoSuchAlgorithmException, IOException {
        compressionEnabled = true;
        createNewBlob("/Users/skystubbeman/Desktop/tester.txt","/Users/skystubbeman/Documents/HTCS_Projects/git");
    }

    public static String createUniqueFileName(String path)
            throws FileNotFoundException, IOException, NoSuchAlgorithmException {
        File file = new File(path);
       
        FileInputStream in = new FileInputStream(file);
        BufferedInputStream br = new BufferedInputStream(in);
        MessageDigest sha1Digest = MessageDigest.getInstance("SHA-1");

        byte[] inputBytes = br.readAllBytes();
        byte[] endingBytes;

        in.close();
        br.close();

        if(compressionEnabled){
            Deflater deflater = new Deflater();
            deflater.setInput(inputBytes);
            deflater.finish();

            byte[] output = new byte[inputBytes.length * 2];
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            while (!deflater.finished()){
                int num = deflater.deflate(output);
                outputStream.write(output, 0, num);
            }
            endingBytes = outputStream.toByteArray();
            outputStream.close();
        }
        else{
           endingBytes = inputBytes;
        }

        sha1Digest.update(endingBytes);
        byte[] hash = sha1Digest.digest();

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < hash.length; i++) {
            sb.append(String.format("%02x", hash[i]));
        }

        return sb.toString();
    }

    public static void createNewBlob(String filePath, String gitRepoPath) throws FileNotFoundException, NoSuchAlgorithmException, IOException{
        
        String name = createUniqueFileName(filePath);
        File file = new File(gitRepoPath + "/objects", name); 
        boolean check = file.createNewFile();
        if (!check){
            System.out.println("blob already exists");
        }
        else{
            BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(filePath));
            BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(file));
           
            int i;
            while ((i = inputStream.read()) != -1){
                outputStream.write(i);
            }

            inputStream.close();
            outputStream.close();
        }
        
        BufferedWriter bw = new BufferedWriter (new FileWriter(gitRepoPath + "/index", true));
        File filePointer = new File(filePath); //huh?
        bw.write(name + " " + filePointer.getName() + "\n"); 
        bw.close();
    }

}
