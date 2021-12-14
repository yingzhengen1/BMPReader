import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.RenderedImage;
import java.io.*;

public class BMP_DCT
{
    public static void main(String[] args)
    {
        EventQueue.invokeLater(new Runnable()
        {
            @Override
            public void run()
            {
                JFrame frame = new ImageViewerFrame();
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setVisible(true);
            }
        });
    }
}

class ImageViewerFrame extends JFrame
{
    private JLabel label1, label2;
    public  JFileChooser chooser;
    private static final int DEFAULT_WIDTH = 1920;
    private static final int DEFAULT_HEIGHT = 1080;

    private String filename;

    public ImageViewerFrame()
    {
        setTitle("Project2 Q2 BMP Image Viewer");
        setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        setLayout(null);

        //Display the original image on the left
        label1 = new JLabel();
        label1.setBounds(0, 0, 960, 540);
        add(label1);

        //Display the resultant image on the right
        label2 = new JLabel();
        label2.setBounds(800, 0, 960, 540);
        add(label2);

        chooser = new JFileChooser();
        chooser.setCurrentDirectory(new File("."));

        JMenuBar menubar = new JMenuBar();
        setJMenuBar(menubar);
        JMenu menu = new JMenu("File");
        menubar.add(menu);
        JMenuItem openItem = new JMenuItem("Open");
        menu.add(openItem);
        JMenuItem exitItem = new JMenuItem("Close");
        menu.add(exitItem);

        openItem.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                int result = chooser.showOpenDialog(null);
                if(result == JFileChooser.APPROVE_OPTION)
                {
                    filename = chooser.getSelectedFile().getPath();
                    File file = new File(filename);
                    Image image = null;
                    try {
                        image = ImageIO.read(file);
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                    ImageIcon originalImage = new ImageIcon(image);
                    originalImage.setImage(originalImage.getImage().getScaledInstance(800,500, Image.SCALE_DEFAULT));
                    label1.setIcon(originalImage);

                    BmpReader bmp = new BmpReader();
                    try {
                        bmp.init(filename);
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                    try {
                        bmp.run(filename);
                    } catch (IOException fileNotFoundException) {
                        fileNotFoundException.printStackTrace();
                    }

                    String newName = filename;
                    newName = newName.substring(0,newName.indexOf("."));
                    newName = newName + "-Modified.bmp";
                    File modifiedFile = new File(newName);
                    Image image2 = null;
                    try {
                        image2 = ImageIO.read(modifiedFile);
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                    ImageIcon modifiedImage = new ImageIcon(image2);
                    modifiedImage.setImage(modifiedImage.getImage().getScaledInstance(800, 500, Image.SCALE_DEFAULT));
                    label2.setIcon(modifiedImage);



                }
            }
        });

        exitItem.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                System.exit(0);

            }
        });

    }

    static class BmpReader
    {
        private static int width;
        private static int height;
        private static int offset;
        private static int bitDepth;
        private static int[][] red, green, blue, Y, U, V;//matrix for RGB and YUV values
        private static int[][] DCTY,DCTU,DCTV;
        private static int[][] QuangtizedY,  QuangtizedU,  QuangtizedV;
        private static int[][] DCTInversedY, DCTInversedU, DCTInversedV;

        private static int[][] QUANTIZATIONTABLE =
        {
        {1, 1, 2, 4, 8, 16, 32, 64},
        {1, 1, 2, 4, 8, 16, 32, 64},
        {2, 2, 2, 4, 8, 16, 32, 64},
        {4, 4, 4, 4, 8, 16, 32, 64},
        {8, 8, 8, 8, 8, 16, 32 ,64},
        {16,16,16,16,16,16, 32, 64},
        {32,32,32,32,32,32, 32, 64},
        {64,64,64,64,64,64, 64, 64}
        };



        private static final int DCTsize = 8;

        public void init(String filepath) throws IOException
        {
            java.io.RandomAccessFile raf = new java.io.RandomAccessFile(filepath,"r");
            //bitmap header information
            byte[] array1 = new byte[14];
            raf.read(array1, 0, 14);

            byte[] array2 = new byte[40];
            raf.read(array2, 0, 40);


            width = toInt(array2, 7);
            height = toInt(array2, 11);
            offset = toInt(array1, 13);
            setColor(raf);

            raf.close();
        }

        //
        public void setColor(java.io.RandomAccessFile raf)throws IOException
        {
            red   = new int[height][width];
            green = new int[height][width];
            blue  = new int[height][width];
            Y     = new int[height][width];
            U     = new int[height][width];
            V     = new int[height][width];

            raf.seek(offset);//move to correct index
            for (int i = height - 1; i >= 0; i--)
            {
                for (int j = 0; j < width; j++)
                {
                    try {
                        // Reminder: BGR order
                        blue[i][j] = raf.read();
                        green[i][j] = raf.read();
                        red[i][j] = raf.read();
                        Y[i][j] = (int) (red[i][j]*0.299)+(int) (green[i][j]*0.587)+ (int) (blue[i][j]*0.114);
                        U[i][j] = (int) (red[i][j]*-0.299)+(int) (green[i][j]*-0.587)+ (int) (blue[i][j]*0.886);
                        V[i][j] = (int) (red[i][j]*0.701)+(int) (green[i][j]*-0.587)+ (int) (blue[i][j]*-0.114);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        //convert 4bytes to an int
        public int toInt(byte[] array2, int start)
        {
            int i = ( (array2[start] & 0xff) << 24)
                    | ((array2[start - 1] & 0xff) << 16)
                    | ((array2[start - 2] & 0xff) << 8)
                    | (array2[start - 3] & 0xff);
            return i;
        }
        
        public void run(String filename) throws IOException
        {
            int totalPixels = height * width;
            DCTInversedY = performDCT(Y,DCTY,QuangtizedY,DCTInversedY);
            DCTInversedU = performDCT(U,DCTU,QuangtizedU,DCTInversedU);
            DCTInversedV = performDCT(V,DCTV,QuangtizedV,DCTInversedV);
            inverseYUV();


            //convert R[][],G[][],B[][] to 1D
            int index = 0;
            byte[]  oneDred   = new byte[width * height],
                    oneDgreen = new byte[width * height],
                    oneDblue  = new byte[width * height],
                    oneDY     = new byte[width * height];

            for (int i = height-1; i >=0 ; i--)
            {
                for (int j = 0; j < width; j++)
                {
                    oneDred[index++] = (byte) red[i][j];
                }
            }
            index = 0;
            for (int i = height-1; i >=0 ; i--)
            {
                for (int j = 0; j < width; j++)
                {
                    oneDgreen[index++] = (byte) green[i][j];
                }
            }
            index = 0;
            for (int i = height-1; i >=0 ; i--)
            {
                for (int j = 0; j < width; j++)
                {
                    oneDblue[index++] = (byte) blue[i][j];
                }
            }
            index = 0;
            for (int i = height-1; i >=0 ; i--)
            {
                for (int j = 0; j < width; j++)
                {
                    oneDY[index++] = (byte)Y[i][j];
                }
            }
            


            String newName = filename;
            newName = newName.substring(0,newName.indexOf("."));
            FileOutputStream fos = new FileOutputStream(newName + "-Modified.bmp");

            FileInputStream fin = new FileInputStream(filename);
            BufferedInputStream bis = new BufferedInputStream(fin);

            byte[] inputArr = new byte[totalPixels * 3 + offset];
            bis.read(inputArr,0 ,54);//read header

            for (int i = offset, j = 0; i < totalPixels * 3  + offset - 3; i += 3, j++)
            {
                inputArr[i] = oneDblue[j];
                inputArr[i+1] = oneDgreen[j];
                inputArr[i+2] = oneDred[j];

            }

            DataOutputStream dos = new DataOutputStream(fos);
            dos.write(inputArr);
            System.out.println("Generates image success");

            dos.flush();
            dos.close();
            fin.close();
            fos.close();

            FileOutputStream fosY  = new FileOutputStream(filename + "Quantized Y result.txt");
            DataOutputStream dosY = new DataOutputStream(fosY);
            FileOutputStream fosU  = new FileOutputStream(filename + "Quantized U result.txt");
            DataOutputStream dosU = new DataOutputStream(fosU);
            FileOutputStream fosV  = new FileOutputStream(filename + "Quantized V result.txt");
            DataOutputStream dosV = new DataOutputStream(fosV);

            dosY.writeChars("-Quantized Y values are: \n");
            String str = "";
            for (int i = 0; i < height; i++)
            {
                for (int j = 0; j < width; j++)
                {
                    str = str + Y[i][j] + " ";
                }
                str+="\n";
                dosY.writeChars(str);
                str = "";
            }
            dosU.writeChars("-Quantized U valuse are: \n");
            str = "";
            for (int i = 0; i < height; i++)
            {
                for (int j = 0; j < width; j++)
                {
                    str = str + U[i][j] + " ";
                }
                str+="\n";
                dosU.writeChars(str);
                str = "";
            }
            dosV.writeChars("-Quantized V valuse are: \n");
            str = "";
            for (int i = 0; i < height; i++)
            {
                for (int j = 0; j < width; j++)
                {
                    str = str + V[i][j] + " ";
                }
                str+="\n";
                dosV.writeChars(str);
                str = "";
            }

            dosY.flush();
            dosY.close();
            fosY.close();
            dosU.flush();
            dosU.close();
            fosU.close();
            dosV.flush();
            dosV.close();
            fosV.close();

/*

            RenderedImage inputImage = ImageIO.read(new File(filename));
            ByteArrayOutputStream baos = new ByteArrayOutputStream(width * height * 3 + 54);
            ImageIO.write(inputImage, "BMP", baos);
            byte[] srcFile = baos.toByteArray();
            int bitDepth = srcFile[28];


            FileInputStream fin = new FileInputStream(filename);
            BufferedInputStream bis = new BufferedInputStream(fin);

            byte[] destFile = new byte[0];
            if(bitDepth == 1)
            {
                destFile = new byte[width * height/3 + 56]; //2 +54
                bis.read(destFile,0,54);

                destFile[54] = 0;
                destFile[55] = 1;

                for (int i = 56, j = 0  ; i < destFile.length; i+=3, j++)
                {
                    destFile[i] = oneDblue[j];
                    destFile[i+1] = oneDgreen[j];
                    destFile[i+2] = oneDred[j];
                }

            }
            else if(bitDepth == 8)
            {
                destFile = new byte[width * height + 1078]; //1024 +54
                bis.read(destFile,0,54);

                byte byteRGB = 0;
                for (int i = 54; i < 1077; i += 4, byteRGB ++)
                {
                    destFile[i] =   byteRGB;
                    destFile[i+1] = byteRGB;
                    destFile[i+2] = byteRGB;
                    destFile[i+3] = 0;
                }
                for (int i = 1078, j = 0  ; i < destFile.length; i+=3, j++)
                {
                    destFile[i] = oneDblue[j];
                    destFile[i+1] = oneDgreen[j];
                    destFile[i+2] = oneDred[j];
                }

            }

            String newName = filename;
            newName = newName.substring(0,newName.indexOf("."));
            FileOutputStream fos = new FileOutputStream(newName + "-Modified.bmp");
            
            DataOutputStream dos = new DataOutputStream(fos);
            dos.write(destFile);
            System.out.println("Generates image success");

            dos.flush();
            dos.close();
            fin.close();
            fos.close();

*/
        }

        public int[][] performDCT(int[][]M,int[][] DCTM, int[][]QM, int[][] DCTIM)
        {
            int[][] DCTResult        = new  int[height][width];
            int[][] quantizedResult   = new int[height][width];
            int[][] DCTInversedResult = new int[height][width];
            int[][] temp = new int[DCTsize][DCTsize];//8*8 temp matrix
            for (int i = 0; i < height; i += 8)
            {
                for (int j = 0; j < width; j += 8)
                {
                    for (int k = 0; k < 8; k++)
                    {
                        for (int l = 0; l < 8; l++)
                        {
                            temp[k][l] = M[i + k][j + l];
                            //System.arraycopy(M[i + k], j, temp[k], 0, 8);
                        }
                    }
                    temp = DCT(temp);
                    for (int k = 0; k < 8; k++)
                    {
                        for (int l = 0; l < 8; l++)
                        {
                            DCTResult[i + k][j + l] = temp[k][l];
                        }
                    }
                    quantize(temp);
                    for (int k = 0; k < 8; k++)
                    {
                        for (int l = 0; l < 8; l++)
                        {
                             quantizedResult[i + k][j + l] = temp[k][l];
                        }
                    }
                    temp = DCTI(temp);
                    for (int k = 0; k < 8; k++)
                    {
                        for (int l = 0; l < 8; l++)
                        {
                           DCTInversedResult[i + k][j + l] = temp[k][l];//inverse DCT values
                        }
                    }
                }

            }
            DCTM  = DCTResult;
            QM    = quantizedResult;
            DCTIM = DCTInversedResult;
            //System.out.println("original matrix is");
            //printMatrix(M);
            //System.out.println("DCT result is: ");
            //printMatrix(DCTM);
            //System.out.println("Quantization result is: ");
            //printMatrix(QM);
            //System.out.println("Inverse DCT is :");
            //printMatrix(DCTIM);
            return DCTInversedResult;
        }

        public void quantize(int[][] M)
        {
            for (int i = 0; i < DCTsize; i++)
            {
                for (int j = 0; j < DCTsize; j++)
                {
                    M[i][j] = (int) Math.round(M[i][j] * 1.0/ QUANTIZATIONTABLE[i][j]);
                    M[i][j] *= QUANTIZATIONTABLE[i][j];//Dequantize
                }

            }
        }

        public int[][] DCTI(int[][] M)
        {
            int[][] result = new int[DCTsize][DCTsize];
            double[][] pixels = new double[DCTsize][DCTsize];
            for (int i = 0; i < DCTsize; i++)
            {
                for (int j = 0; j < DCTsize; j++)
                {
                    pixels[i][j] = M[i][j];
                }
            }

            double[][] coeff = coefficient();
            double[][] coeff_transpose = transpose(coeff);

            double[][] temp;
            temp   = matrixMultiply(coeff_transpose, pixels);//C = T_transposeY
            pixels = matrixMultiply(temp, coeff);//X = CT

            for (int i = 0; i < DCTsize; i++)
            {
                for (int j = 0; j < DCTsize; j++)
                {
                    result[i][j] = (int) Math.round(pixels[i][j]);
                }
            }
            return result;
        }
        public int[][] DCT(int[][] M)
        {
            int[][] result   = new int[DCTsize][DCTsize];
            double[][] pixels = new double[DCTsize][DCTsize];
            for (int i = 0; i < DCTsize; i++)
            {
                for (int j = 0; j < DCTsize; j++)
                {
                    pixels[i][j] = M[i][j];
                }
            }
            double[][] coeff = coefficient();
            double[][] coeff_transpose = transpose(coeff);

            double[][] temp;
            temp   = matrixMultiply(coeff, pixels);//A = TX
            pixels = matrixMultiply(temp, coeff_transpose);//Y = AT_transpose

            for (int i = 0; i < DCTsize; i++)
            {
                for (int j = 0; j < DCTsize; j++)
                {
                    result[i][j] = (int) Math.round(pixels[i][j]);

                }
            }
            return result;

        }

        //8x8 DCT matrix coefficients
        public static double[][] coefficient()
        {
            double[][] coeff = new double[DCTsize][DCTsize];
            double a0 = 1.0 / Math.sqrt(DCTsize);
            double a  = Math.sqrt(2.0 / DCTsize);
            for(int i = 0; i < DCTsize; i++)
            {
                coeff[0][i] = a0;
            }
            for(int i = 1; i < DCTsize; i++)//row first
            {
                for(int j = 0; j < DCTsize; j++)
                {
                    coeff[i][j] = a * Math.cos((j + 0.5) * i * Math.PI  / (double)DCTsize);
                }
            }
            return coeff;
        }


        public static double[][] transpose(double[][] M)
        {
            double[][] M_trans = new double[DCTsize][DCTsize];
            for (int i = 0; i < DCTsize; i++)
            {
                for (int j = 0; j < DCTsize; j++)
                {
                    M_trans[i][j] = M[j][i];
                }
            }
            return M_trans;
        }
        public static double[][] matrixMultiply(double[][]M, double[][]N)
        {
            double[][] result = new double[DCTsize][DCTsize];
            double temp;
            for (int i = 0; i < DCTsize; i++)
            {
                for (int j = 0; j < DCTsize; j++)
                {
                    temp = 0;
                    for (int k = 0; k < DCTsize; k++)
                    {
                        temp += M[i][k] * N[k][j];
                    }
                    result [i][j] = temp;
                }
            }
            return result;
        }

        public static void inverseYUV()
        {
            for (int i = 0; i < height ; i++)
            {
                for (int j = 0; j < width; j++)
                {
                    red[i][j]   = (int) (1.0   * DCTInversedY[i][j] + 1.0  * DCTInversedV[i][j]);
                    green[i][j] = (int) (0.996 * DCTInversedY[i][j] -0.198 * DCTInversedU[i][j] -0.509 * DCTInversedV[i][j]);
                    blue[i][j]  = (int) (1.02  * DCTInversedY[i][j] + 1.02 * DCTInversedU[i][j]);
                    //System.out.print(red[i][j] + " "+ green[i][j] + " " + blue[i][j]+ " ");
                    red[i][j] = rescale(red[i][j]);
                    green[i][j] = rescale(green[i][j]);
                    blue[i][j] = rescale(blue[i][j]);
                }
                //System.out.println();
            }
        }
        public static int rescale(int n)
        {
            if (n < 0)
                n = 0;
            else if(n > 255)
                n = 255;
            return n;
        }
        public static void printMatrix(int[][]M)
        {
            int height = M.length;
            int width = M[0].length;
            for (int i = 0; i < height; i++) {
                for (int j = 0; j < width; j++) {
                    System.out.print(M[i][j] + " ");
                }
                System.out.println();
            }
            System.out.println();
        }

    }
}

















