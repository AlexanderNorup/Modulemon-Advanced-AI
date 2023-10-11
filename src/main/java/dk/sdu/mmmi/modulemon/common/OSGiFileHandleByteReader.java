package dk.sdu.mmmi.modulemon.common;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class OSGiFileHandleByteReader extends OSGiFileHandle {

    private byte[] cachedBytes;

    public OSGiFileHandleByteReader(String resourceName, Class classToPullFrom) {
        super(resourceName, classToPullFrom);
    }

    @Override
    public byte[] readBytes() {
        if(cachedBytes != null){
            return cachedBytes;
        }
        //From https://stackoverflow.com/a/1264737
        InputStream is = this.read();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int nRead;
        byte[] data = new byte[(int)Math.pow(2,14)]; //Magical number, what?!

        try {
            while ((nRead = is.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
        }catch(Exception ex){
            System.out.println("Loading bytes failed for: " + super.resourceName);
        }

        cachedBytes = buffer.toByteArray();
        return cachedBytes;
    }
}
