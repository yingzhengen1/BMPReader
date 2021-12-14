import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.zip.Deflater;

public class WAV
{
    public static void main(String[] args)
    {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                JFrame frame = new WavViewerFrame();
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setVisible(true);
            }
        });
    }
}

class WavViewerFrame extends JFrame
{
    private JLabel label1 = new JLabel();
    public  JFileChooser chooser;
    private static final int DEFAULT_WIDTH = 600;
    private static final int DEFAULT_HEIGHT = 400;

    private String filename;

    public WavViewerFrame()
    {
        setTitle("Project2 Q1 Wav file viewer");
        setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        setLayout(null);

        chooser = new JFileChooser();
        chooser.setFileFilter(new FileFilter()
        {
            @Override
            public boolean accept(File f)
            {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(".wav");
            }

            @Override
            public String getDescription()
            {
                return "*.wav(audio file)";
            }
        });
        chooser.setCurrentDirectory(new File("."));

        JMenuBar menubar = new JMenuBar();
        setJMenuBar(menubar);
        JMenu menu = new JMenu("File");
        menubar.add(menu);
        JMenuItem openItem = new JMenuItem("Open");
        menu.add(openItem);
        JMenuItem exitItem = new JMenuItem("Close");
        menu.add(exitItem);
        this.add(label1);

        openItem.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                int result = chooser.showOpenDialog(null);
                if(result == JFileChooser.APPROVE_OPTION)
                {
                    filename = chooser.getSelectedFile().getPath();


                    try
                    {
                        WavReader wav = new WavReader(filename);
                        wav.initReader(filename);
                        wav.compression();
                        wav.writeWav();

                        label1.setText("The compression ratio is : " + wav.getCompressionRatio());
                        label1.setBounds(50,50,400,50);

                    } catch (IOException ioException)
                    {
                        ioException.printStackTrace();
                    }



                }
            }
        });


    }



}

class WavReader
{
    private String filename = null;
    private int[][] data    = null;
    private int len = 0;
    private double compressionRatio = 0;

    private int[] singleChannel = null;
    private int[] leftChannel   = null;
    private int[] rightChannel  = null;
    private int[] sumChannel    = null;
    private int[] sideChannel   = null;

    private byte[] compressedData = null;
    private int compressedDataLen = 0;



    private String RIFFdescriptor = null;
    private static  int lenRIFFdecriptor = 4;

    private long chunkSize = 0;
    private static  int lenChunkSize = 4;

    private String waveFlag = null;
    private static int lenWaveFlag = 4;

    private String fmtChunkID = null;
    private static int lenFmtChunkID = 4;

    private long subChunk1Size = 0;
    private static int lenSubChunk1Size = 4;

    private int audioFormat = 0;
    private static int lenAudioFormat = 2;

    private int numChannels = 0;
    private static int lenNumChannels = 2;

    private long sampleRate = 0;
    private static int lenSapleRate = 2;

    private long byteRate = 0;
    private static int lenByteRate = 4;

    private int blockAlign = 0;
    private static  int lenBlockAlign = 2;

    private int bitsPerSample = 0;
    private static  int lenBitsPerSample = 2;

    private String dataSubchunk = null;
    private static  int lenDataSubchunk = 4;

    private long subchunk2Size = 0;
    private static  int lenSubchunk2Size = 4;

    private FileInputStream fis = null;
    private BufferedInputStream bis = null;

    private boolean issuccess = false;

    public WavReader(String filename) throws IOException
    {
        this.initReader(filename);
    }

    public boolean isSuccess()
    {
        return issuccess;
    }

    public int getBitsPerSample()
    {
        return bitsPerSample;
    }

    public long getSampleRate()
    {
        return sampleRate;
    }

    public int getNumChannels()
    {
        return numChannels;
    }

    public int[][] getData()
    {
        return data;
    }

    public int getDataLen()
    {
        return len;
    }

    public double getCompressionRatio()
    {
        return compressionRatio;
    }

    public String readString(int len) throws IOException
    {
        byte[] buffer = new byte[len];
        bis.read(buffer);
        return new String(buffer);
    }

    public int read2BytesToInt()
    {
        byte[] buffer = new byte[2];
        int result = 0;
        try
        {
            if(bis.read(buffer)!=2)
                throw new IOException("No more data!");
            result = (buffer[0] & 0x000000FF) | (((int)buffer[1]) << 8);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return result;

    }

    public long readLong() throws IOException
    {
        long result = 0;
        long[] l = new long[4];
        for(int i=0; i<4; ++i)
        {
            l[i] = bis.read();
        }
        result = l[0] | (l[1]<<8) | (l[2]<<16) | (l[3]<<24);
        return result;
    }

    public byte[] readBytes(int Len) throws IOException
    {
        byte[] buffer = new byte[len];
        bis.read(buffer);
        return buffer;
    }
    public void initReader(String filename) throws IOException
    {
        this.filename = filename;
        fis = new FileInputStream(filename);
        bis = new BufferedInputStream(fis);

        this.RIFFdescriptor = readString(lenRIFFdecriptor);
        this.chunkSize = readLong();
        this.waveFlag  = readString(lenWaveFlag);

        this.fmtChunkID = readString(lenFmtChunkID);
        this.subChunk1Size = readLong();
        this.audioFormat   = read2BytesToInt();
        this.numChannels   = read2BytesToInt();
        this.sampleRate    = readLong();
        this.byteRate      = readLong();
        this.blockAlign    = read2BytesToInt();
        this.bitsPerSample = read2BytesToInt();

        this.dataSubchunk  = readString(lenDataSubchunk);
        this.subchunk2Size = readLong();

        this.len = (int) (this.subchunk2Size/(this.bitsPerSample/8)/this.numChannels);
        this.data = new int[numChannels][this.len];

       // System.out.println("Read data: ");
        for (int i = 0; i < this.len; i++)
        {
            for (int n = 0; n < this.numChannels; n++)
            {
                if(this.bitsPerSample == 8)
                    this.data[n][i] = bis.read();
                else if(this.bitsPerSample == 16)
                    this.data[n][i] = this.read2BytesToInt();
                //System.out.print(this.data[n][i]+ " ");
            }
        }

        int size = data[0].length;
        if(this.numChannels == 1)
        {
            singleChannel = new int[size];
            for (int i = 0; i < size; i++)
            {
                singleChannel[i] = data[0][i];
            }
        }
        else if(this.numChannels == 2)
        {
            leftChannel  = new int[size];
            rightChannel = new int[size];
            for (int i = 0; i < size; i++)
            {
                leftChannel[i]  = data[0][i];
                rightChannel[i] = data[1][i];
            }
        }
        issuccess = true;

        bis.close();
        fis.close();


    }


    public int[] linearPredict(int[] channel)
    {
        int size = channel.length;
        int[] result = new int[size];
        result[0] = channel[0];
        //result[1] = channel[1];
        int px, xMinus1, xMinus2;

        //System.out.println("prediction error:");
        for (int i = 2; i < size; i++)
        {
            xMinus1 = channel[i] ;
            xMinus2 = channel[i - 1] ;
            px = channel[i] - ((2 * xMinus1) - xMinus2);//prediction error
            //px = channel[i] - Math.floorDiv((xMinus1 + xMinus2) ,2);
            result[i] = px;
            //System.out.print(px + " ");

        }
        return result;
    }

    public void channelCoupling()
    {
        int size = data[0].length;
        sumChannel  = new int[size];
        sideChannel = new int[size];

        for (int i = 0; i < size; i++)
        {
            sumChannel[i]  = (int) (1.0 * (leftChannel[i] + rightChannel[i]))/2;
            sideChannel[i] = (int) (1.0 * (leftChannel[i] - rightChannel[i]))/2;
            System.out.print(sumChannel[i] + " ");
            System.out.print(sideChannel[i] + " ");
        }
    }

    public byte[] entropyEncoding(int[] channel)
    {
        byte[] tempByte = intArr2ByteArr(channel);
        byte[] buffer = new byte[tempByte.length];

        Deflater deflater = new Deflater();
        deflater.setInput(tempByte);
        deflater.finish();


        compressedDataLen = deflater.deflate(buffer);


        byte[] output = new byte[compressedDataLen];
        for (int i = 0; i < compressedDataLen; i++)
        {
            output[i] = buffer[i];
        }

        return output;
    }

    public byte[] intArr2ByteArr(int[] intArr)
    {
        int size = intArr.length * 4;
        byte[] result = new byte[size];
        int current = 0;
        for (int i = 0, j = 0; i < intArr.length; i++, j+=4)
        {
            current = intArr[i];
            result[j]     = (byte) ((current>>24) & 0b1111_1111);
            result[j + 1] = (byte) ((current>>16) & 0b1111_1111);
            result[j + 2] = (byte) ((current>>8)  & 0b1111_1111);
            result[j + 3] = (byte) ((current>>0)  & 0b1111_1111);
        }
        return result;
    }

    public int[] byteArr2IntArr(byte[] byteArr)
    {
        if(byteArr.length % 4 != 0)
        {
            System.out.println("Failed to covert byte array to int array.");
        }
        int[] result = new int[byteArr.length / 4];
        int i1,i2,i3,i4;
        for (int i = 0, j = 0  ; i < result.length; i++, j+=4)
        {
            i1 = byteArr[j];
            i2 = byteArr[j + 1];
            i3 = byteArr[j + 2];
            i4 = byteArr[j + 3];
            if(i1 < 0)
                i1 += 256;
            if(i2 < 0)
                i2 += 256;
            if(i3 < 0)
                i3 += 256;
            if(i4 < 0)
                i4 += 256;

            result[i] = (i1 << 24) + (i2 << 16) + (i3 << 8) + (i4 << 0);

        }
        return result;
    }

    public void compression()
    {
        int[] tempData = null;
        if(numChannels == 2)
        {
            channelCoupling();
            tempData = new int[sideChannel.length + sumChannel.length];
            for (int i = 0, j = 0; i < sumChannel.length; i++, j+=2)
            {
                tempData[j]   = sumChannel[i];
                tempData[j+1] = sideChannel[i];
            }
        }
        else if(numChannels == 1)
        {
            tempData = new  int[singleChannel.length];
            for (int i = 0; i < singleChannel.length; i++)
            {
                tempData[i] = singleChannel[i];
            }
        }
        tempData = linearPredict(tempData);

        compressedData = entropyEncoding(tempData);
    }

    public void writeWav() throws IOException
    {
        String newName = filename;
        newName = newName.substring(0,newName.indexOf("."));
        newName += "-CompressedData.txt";
        FileOutputStream fos = new FileOutputStream(newName);

        FileInputStream fin = new FileInputStream(filename);
        BufferedInputStream bis = new BufferedInputStream(fin);


        byte[] outputArr = new byte[compressedData.length];//RIFF + Format + compressed DATA

        for (int i = 0; i < outputArr.length; i++)
        {
            outputArr[i] = compressedData[i];
        }

        DataOutputStream dos = new DataOutputStream(fos);
        dos.write(outputArr);
        System.out.println("Compression success!");

        dos.flush();
        dos.close();
        fin.close();
        fos.close();

        File file  = new File(filename);
        File file1 = new File(newName);
        System.out.println("Compressed file length is : " + file1.length());
        System.out.println("Original file length is : " + file.length());
        compressionRatio = (1.0*file1.length()/file.length());
        System.out.println("The compression ratio is " + compressionRatio);


    }





}


