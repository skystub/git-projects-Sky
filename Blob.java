import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Blob {
    public static void main(String[] args) {
        
    }

    public static String createUniqueFileName(File file) throws FileNotFoundException, IOException, NoSuchAlgorithmException{
        
        FileInputStream in = new FileInputStream(file);
        BufferedInputStream buff = new BufferedInputStream(in);
        MessageDigest sha1Digest = MessageDigest.getInstance("SHA-1");

        byte[] byteArr = new byte[(int) file.length()]; //works?

        int length = buff.read(byteArr);

        byte[] hash = sha1Digest.digest(byteArr);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++){
            sb.append((char) hash[i]);
        }

        return sb.toString();
    }
}
