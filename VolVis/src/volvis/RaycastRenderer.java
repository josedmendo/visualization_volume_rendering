/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package volvis;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.awt.AWTTextureIO;
import gui.RaycastRendererPanel;
import gui.TransferFunction2DEditor;
import gui.TransferFunctionEditor;
import java.awt.image.BufferedImage;

import org.omg.PortableInterceptor.SYSTEM_EXCEPTION;
import util.TFChangeListener;
import util.VectorMath;
import volume.GradientVolume;
import volume.Volume;
import volume.VoxelGradient;

/**
 *
 * @author michel
 */
public class RaycastRenderer extends Renderer implements TFChangeListener {

    private Volume volume = null;
    private GradientVolume gradients = null;
    RaycastRendererPanel panel;
    TransferFunction tFunc;
    TransferFunctionEditor tfEditor;
    TransferFunction2DEditor tfEditor2D;
    private boolean phongFlag=false;
    private boolean tf2dFlag = false;
    int stepInteractive = 10;


    public enum RENDER_METHOD {
        SLICER, MIP, COMPOSITING, TF2D, PHONG
    }
    private RENDER_METHOD method;

    public RaycastRenderer() {
        method = RENDER_METHOD.SLICER;
        panel = new RaycastRendererPanel(this);
        panel.setSpeedLabel("0");
    }

    public void setRenderMethod(RENDER_METHOD method) {
        this.method = method;
        changed();
    }

    public void setVolume(Volume vol) {
        System.out.println("Assigning volume");
        volume = vol;

        System.out.println("Computing gradients");
        gradients = new GradientVolume(vol);

        // set up image for storing the resulting rendering
        // the image width and height are equal to the length of the volume diagonal
        int imageSize = (int) Math.floor(Math.sqrt(vol.getDimX() * vol.getDimX() + vol.getDimY() * vol.getDimY()
                + vol.getDimZ() * vol.getDimZ()));
        if (imageSize % 2 != 0) {
            imageSize = imageSize + 1;
        }
        image = new BufferedImage(imageSize, imageSize, BufferedImage.TYPE_INT_ARGB);
        // create a standard TF where lowest intensity maps to black, the highest to white, and opacity increases
        // linearly from 0.0 to 1.0 over the intensity range
        tFunc = new TransferFunction(volume.getMinimum(), volume.getMaximum());
        
        // uncomment this to initialize the TF with good starting values for the orange dataset 
        tFunc.setTestFunc();
        
        
        tFunc.addTFChangeListener(this);
        tfEditor = new TransferFunctionEditor(tFunc, volume.getHistogram());
        
        tfEditor2D = new TransferFunction2DEditor(volume, gradients);
        tfEditor2D.addTFChangeListener(this);

        System.out.println("Finished initialization of RaycastRenderer");
    }

    public RaycastRendererPanel getPanel() {
        return panel;
    }

    public TransferFunction2DEditor getTF2DPanel() {
        return tfEditor2D;
    }
    
    public TransferFunctionEditor getTFPanel() {
        return tfEditor;
    }
     
    void toggle(){
        phongFlag = !phongFlag;
    }

    TFColor phong(TFColor voxelColor, double[] viewVec,double[] pixelCoord){

        if (pixelCoord[0] < 0 || pixelCoord[0] >= volume.getDimX() || pixelCoord[1] < 0 || pixelCoord[1] >= volume.getDimY()
                || pixelCoord[2] < 0 || pixelCoord[2] >= volume.getDimZ()) {
            return voxelColor;
        }

       double kAmb = 0.1;
       double kDiff = 0.7;
       double kSpec = 0.2;
       int alpha = 10;
       double []L = {-viewVec[0],-viewVec[1],-viewVec[2]};
        VoxelGradient voxelGradient = gradients.getGradient((int) Math.floor(pixelCoord[0]),
               (int) Math.floor(pixelCoord[1]),(int) Math.floor(pixelCoord[2]));
        double []N = {voxelGradient.x/voxelGradient.mag,
                voxelGradient.y/voxelGradient.mag,voxelGradient.z/voxelGradient.mag};
        //double []R = VectorMath.substractProduct(VectorMath.scalarProduct(VectorMath.dotproduct(VectorMath.scalarProduct(2,N),L),N),L);
        //i = kamb + kdiff(L*N)+kspec(V*R)
        double lDotN = VectorMath.dotproduct(L,N);
        //H = L
        double nDotH = VectorMath.dotproduct(N,L);

        if(lDotN >0 && nDotH >0){
            voxelColor.r = kAmb + voxelColor.r*kDiff*lDotN  + kSpec*Math.pow(nDotH,alpha);
            voxelColor.g = kAmb + voxelColor.g*kDiff*lDotN  + kSpec*Math.pow(nDotH,alpha);
            voxelColor.b = kAmb + voxelColor.b*kDiff*lDotN  + kSpec*Math.pow(nDotH,alpha);
        }

        return voxelColor;

    }
    short getVoxel(double[] coord) {

        if (coord[0] < 0 || coord[0] >= volume.getDimX() || coord[1] < 0 || coord[1] >= volume.getDimY()
                || coord[2] < 0 || coord[2] >= volume.getDimZ()) {
            return 0;
        }

        int x = (int) Math.floor(coord[0]);
        int y = (int) Math.floor(coord[1]);
        int z = (int) Math.floor(coord[2]);

        return volume.getVoxel(x, y, z);
    }

    float getGradient(double[] coord) {

        if (coord[0] < 0 || coord[0] >= volume.getDimX() || coord[1] < 0 || coord[1] >= volume.getDimY()
                || coord[2] < 0 || coord[2] >= volume.getDimZ()) {
            return 0;
        }

        int x = (int) Math.floor(coord[0]);
        int y = (int) Math.floor(coord[1]);
        int z = (int) Math.floor(coord[2]);

        return gradients.getGradient(x, y, z).mag;
    }

    short interpolateVoxel(double[] coord) {

        if (coord[0] < 0 || coord[0] >= volume.getDimX() || coord[1] < 0 || coord[1] >= volume.getDimY()
                || coord[2] < 0 || coord[2] >= volume.getDimZ()) {
            return 0;
        }

        //get adjacent voxels

        int xLow = (int) Math.floor(coord[0]);
        int yLow = (int) Math.floor(coord[1]);
        int zLow = (int) Math.floor(coord[2]);
        int xHigh = (int) Math.ceil(coord[0]);
        int yHigh = (int) Math.ceil(coord[1]);
        int zHigh = (int) Math.ceil(coord[2]);


    /*    if (xLow < 0 || xHigh >= volume.getDimX() || yLow < 0 || yHigh >= volume.getDimY()
                || zLow < 0 || zHigh >= volume.getDimZ()) {
            return 0;
        }//maybe it goes off limits, xHigh > getDim()?  */

        if (xHigh > volume.getDimX() - 1 || yHigh > volume.getDimY() - 1 || zHigh > volume.getDimZ() - 1) {
            if (xHigh > volume.getDimX() - 1) {
                xLow = volume.getDimX() - 1;
            }
            if (yHigh > volume.getDimY() - 1) {
                yLow = volume.getDimY() - 1;
            }
            if (zHigh > volume.getDimZ() - 1) {
                zLow = volume.getDimZ() - 1;
            }
            return volume.getVoxel(xLow, yLow, zLow);
        }

        double alpha = ((coord[0] == xLow) ? 0 :  (coord[0]-xLow)/(xHigh-xLow));
        double beta = ((coord[1] == yLow) ? 0 :  (coord[1]-yLow)/(yHigh-yLow));
        double gamma = ((coord[2] == zLow) ? 0 :  (coord[2]-zLow)/(zHigh-zLow));

        //xLow,yLow,zLow
        int s000 = volume.getVoxel(xLow,yLow,zLow);
        //xLow,yLow,zHigh
        int s001 =  volume.getVoxel(xLow,yLow,zHigh);
        //xLow,yHigh,zLow
        int s010 =  volume.getVoxel(xLow,yHigh,zLow);
        //xLow,yHigh,zHigh
        int s011 =  volume.getVoxel(xLow,yHigh,zHigh);
        //xHigh,yLow,zLow
        int s100 =  volume.getVoxel(xHigh,yLow,zLow);
        //xHigh,yLow,zHigh
        int s101 =  volume.getVoxel(xHigh,yLow,zHigh);
        //xHigh,yHigh,zLow
        int s110 =  volume.getVoxel(xHigh,yHigh,zLow);
        //xHigh,yHigh,zHigh
        int s111 =  volume.getVoxel(xHigh,yHigh,zHigh);

        short inter = (short)((1 - alpha)*(1 - beta)*(1 - gamma)*s010 +
                alpha*(1 - beta)*(1 - gamma)*s110+
                (1-alpha)*beta*(1-gamma)*s000+
                alpha*beta*(1-gamma)*s100+
                (1-alpha)*(1-beta)*gamma*s011+
                alpha*(1-beta)*gamma*s111+
                (1-alpha)*beta*gamma*s001+
                alpha*beta*gamma*s101);

        return inter;
    }


    float interpolateGradient(double[] coord) {

        if (coord[0] < 0 || coord[0] >= volume.getDimX() || coord[1] < 0 || coord[1] >= volume.getDimY()
                || coord[2] < 0 || coord[2] >= volume.getDimZ()) {
            return 0;
        }

        //get adjacent voxels

        int xLow = (int) Math.floor(coord[0]);
        int yLow = (int) Math.floor(coord[1]);
        int zLow = (int) Math.floor(coord[2]);
        int xHigh = (int) Math.ceil(coord[0]);
        int yHigh = (int) Math.ceil(coord[1]);
        int zHigh = (int) Math.ceil(coord[2]);

     /*   if (xLow < 0 || xHigh >= volume.getDimX() || yLow < 0 || yHigh >= volume.getDimY()
                || zLow < 0 || zHigh >= volume.getDimZ()) {
            return 0;
        }//maybe it goes off limits, xHigh > getDim()? */

        if (xHigh > volume.getDimX() - 1 || yHigh > volume.getDimY() - 1 || zHigh > volume.getDimZ() - 1) {
            if (xHigh > volume.getDimX() - 1) {
                xLow = volume.getDimX() - 1;
            }
            if (yHigh > volume.getDimY() - 1) {
                yLow = volume.getDimY() - 1;
            }
            if (zHigh > volume.getDimZ() - 1) {
                zLow = volume.getDimZ() - 1;
            }
            return volume.getVoxel(xLow, yLow, zLow);
        }

        double alpha = ((coord[0] == xLow) ? 0 :  (coord[0]-xLow)/(xHigh-xLow));
        double beta = ((coord[1] == yLow) ? 0 :  (coord[1]-yLow)/(yHigh-yLow));
        double gamma = ((coord[2] == zLow) ? 0 :  (coord[2]-zLow)/(zHigh-zLow));

        //xLow,yLow,zLow
        float s000 = gradients.getGradient(xLow,yLow,zLow).mag;
        //xLow,yLow,zHigh
        float s001 =  gradients.getGradient(xLow,yLow,zHigh).mag;
        //xLow,yHigh,zLow
        float s010 =  gradients.getGradient(xLow,yHigh,zLow).mag;
        //xLow,yHigh,zHigh
        float s011 =  gradients.getGradient(xLow,yHigh,zHigh).mag;
        //xHigh,yLow,zLow
        float s100 =  gradients.getGradient(xHigh,yLow,zLow).mag;
        //xHigh,yLow,zHigh
        float s101 =  gradients.getGradient(xHigh,yLow,zHigh).mag;
        //xHigh,yHigh,zLow
        float s110 =  gradients.getGradient(xHigh,yHigh,zLow).mag;
        //xHigh,yHigh,zHigh
        float s111 =  gradients.getGradient(xHigh,yHigh,zHigh).mag;

        return (float)((1 - alpha)*(1 - beta)*(1 - gamma)*s010 +
                alpha*(1 - beta)*(1 - gamma)*s110+
                (1-alpha)*beta*(1-gamma)*s000+
                alpha*beta*(1-gamma)*s100+
                (1-alpha)*(1-beta)*gamma*s011+
                alpha*(1-beta)*gamma*s111+
                (1-alpha)*beta*gamma*s001+
                alpha*beta*gamma*s101);


    }

    void clear() {
        // clear image
        for (int j = 0; j < image.getHeight(); j++) {
            for (int i = 0; i < image.getWidth(); i++) {
                image.setRGB(i, j, 0);
            }
        }
    }

    // SLICER method
    void slicer(double[] viewMatrix) {
        // clear image
        clear();
        // vector uVec and vVec define a plane through the origin, 
        // perpendicular to the view vector viewVec
        double[] viewVec = new double[3];
        double[] uVec = new double[3];
        double[] vVec = new double[3];
        VectorMath.setVector(viewVec, viewMatrix[2], viewMatrix[6], viewMatrix[10]);//(1,0,0)
        VectorMath.setVector(uVec, viewMatrix[0], viewMatrix[4], viewMatrix[8]);//(0,1,0)
        VectorMath.setVector(vVec, viewMatrix[1], viewMatrix[5], viewMatrix[9]);//(0,0,1)

        // image is square
        int imageCenter = image.getWidth() / 2;

        double[] pixelCoord = new double[3];
        double[] volumeCenter = new double[3];
        VectorMath.setVector(volumeCenter, volume.getDimX() / 2, volume.getDimY() / 2, volume.getDimZ() / 2);

        // sample on a plane through the origin of the volume data
        double max = volume.getMaximum();
        TFColor voxelColor = new TFColor();

        for (int j = 0; j < image.getHeight(); j++) {
            for (int i = 0; i < image.getWidth(); i++) {

                pixelCoord[0] = uVec[0] * (i - imageCenter) + vVec[0] * (j - imageCenter)
                        + volumeCenter[0];
                pixelCoord[1] = uVec[1] * (i - imageCenter) + vVec[1] * (j - imageCenter)
                        + volumeCenter[1];
                pixelCoord[2] = uVec[2] * (i - imageCenter) + vVec[2] * (j - imageCenter)
                        + volumeCenter[2];

                // INTERPOLATE HERE!!!!!!
                int val = interpolateVoxel(pixelCoord);
                //int val = getVoxel(pixelCoord);
                
                // Map the intensity to a grey value by linear scaling
                voxelColor.r = val/max;
                voxelColor.g = voxelColor.r;
                voxelColor.b = voxelColor.r;
                voxelColor.a = val > 0 ? 1.0 : 0.0;  // this makes intensity 0 completely transparent and the rest opaque
                // Alternatively, apply the transfer function to obtain a color
                // voxelColor = tFunc.getColor(val);
                
                
                // BufferedImage expects a pixel color packed as ARGB in an int
                int c_alpha = voxelColor.a <= 1.0 ? (int) Math.floor(voxelColor.a * 255) : 255;
                int c_red = voxelColor.r <= 1.0 ? (int) Math.floor(voxelColor.r * 255) : 255;
                int c_green = voxelColor.g <= 1.0 ? (int) Math.floor(voxelColor.g * 255) : 255;
                int c_blue = voxelColor.b <= 1.0 ? (int) Math.floor(voxelColor.b * 255) : 255;
                int pixelColor = (c_alpha << 24) | (c_red << 16) | (c_green << 8) | c_blue;
                image.setRGB(i, j, pixelColor);
            }
        }

    }

    // MIP method
    void mip(double[] viewMatrix) {
        // clear image
        clear();
        // vector uVec and vVec define a plane through the origin,
        // perpendicular to the view vector viewVec
        double[] viewVec = new double[3];
        double[] uVec = new double[3];
        double[] vVec = new double[3];
        VectorMath.setVector(viewVec, viewMatrix[2], viewMatrix[6], viewMatrix[10]);
        VectorMath.setVector(uVec, viewMatrix[0], viewMatrix[4], viewMatrix[8]);
        VectorMath.setVector(vVec, viewMatrix[1], viewMatrix[5], viewMatrix[9]);

        // image is square
        int imageCenter = image.getWidth() / 2;
        double diagonal = volume.getDiagonal();

        double[] pixelCoord = new double[3];
        double[] volumeCenter = new double[3];
        VectorMath.setVector(volumeCenter, volume.getDimX() / 2, volume.getDimY() / 2, volume.getDimZ() / 2);

        // sample on a plane through the origin of the volume data
        double max = volume.getMaximum();
        TFColor voxelColor = new TFColor();

        // vector vals to store the values through axis Z (k index)
        short val;

        // step interactive
        int step = (interactiveMode) ? stepInteractive : 1;

        for (int j = 0; j < image.getHeight(); j++) {
            for (int i = 0; i < image.getWidth(); i++) {
                short max_val=0;
                for(double k = -diagonal/2; k < diagonal/2; k+=step) {

                    pixelCoord[0] = uVec[0] * (i - imageCenter) + vVec[0] * (j - imageCenter)
                            + viewVec[0]*k + volumeCenter[0];
                    pixelCoord[1] = uVec[1] * (i - imageCenter) + vVec[1] * (j - imageCenter)
                            + viewVec[1]*k + volumeCenter[1];
                    pixelCoord[2] = uVec[2] * (i - imageCenter) + vVec[2] * (j - imageCenter)
                            + viewVec[2]*k + volumeCenter[2];
                    // getVoxel
                    //val = getVoxel(pixelCoord);
                    val = interpolateVoxel(pixelCoord);
                    if(val > max_val){
                        max_val = val;
                    }
                }

                // Map the intensity to a grey value by linear scaling
                voxelColor.r = max_val / max;
                voxelColor.g = voxelColor.r;
                voxelColor.b = voxelColor.r;
                voxelColor.a = max_val > 0 ? 1.0 : 0.0;  // this makes intensity 0 completely transparent and the rest opaque
                // Alternatively, apply the transfer function to obtain a color
                // voxelColor = tFunc.getColor(max_val);

                // BufferedImage expects a pixel color packed as ARGB in an int
                int c_alpha = voxelColor.a <= 1.0 ? (int) Math.floor(voxelColor.a * 255) : 255;
                int c_red = voxelColor.r <= 1.0 ? (int) Math.floor(voxelColor.r * 255) : 255;
                int c_green = voxelColor.g <= 1.0 ? (int) Math.floor(voxelColor.g * 255) : 255;
                int c_blue = voxelColor.b <= 1.0 ? (int) Math.floor(voxelColor.b * 255) : 255;
                int pixelColor = (c_alpha << 24) | (c_red << 16) | (c_green << 8) | c_blue;
                image.setRGB(i, j, pixelColor);

            }
        }


    }


///////////////////////////////////////////////////////////////////////////////////////////////////
    // COMPOSITING method
    void compositing(double[] viewMatrix) {
        // clear image
        clear();

        // new variables
        TFColor compositeColor;
        TFColor voxelColor;

        // vector uVec and vVec define a plane through the origin,
        // perpendicular to the view vector viewVec
        double[] viewVec = new double[3];
        double[] uVec = new double[3];
        double[] vVec = new double[3];
        VectorMath.setVector(viewVec, viewMatrix[2], viewMatrix[6], viewMatrix[10]);
        VectorMath.setVector(uVec, viewMatrix[0], viewMatrix[4], viewMatrix[8]);
        VectorMath.setVector(vVec, viewMatrix[1], viewMatrix[5], viewMatrix[9]);

        // image is square
        int imageCenter = image.getWidth() / 2;
        double diagonal = volume.getDiagonal();

        double[] pixelCoord = new double[3];
        double[] volumeCenter = new double[3];
        VectorMath.setVector(volumeCenter, volume.getDimX() / 2, volume.getDimY() / 2, volume.getDimZ() / 2);

        // step interactive
        int step = (interactiveMode) ? stepInteractive : 1;

        for (int j = 0; j < image.getHeight(); j++) {
            for (int i = 0; i < image.getWidth(); i++) {
                // initialize color
                compositeColor = new TFColor(0,0,0,1);
                for(double k = -diagonal/2; k < diagonal/2; k+=step) {

                    pixelCoord[0] = uVec[0] * (i - imageCenter) + vVec[0] * (j - imageCenter)
                            + viewVec[0]*k + volumeCenter[0];
                    pixelCoord[1] = uVec[1] * (i - imageCenter) + vVec[1] * (j - imageCenter)
                            + viewVec[1]*k + volumeCenter[1];
                    pixelCoord[2] = uVec[2] * (i - imageCenter) + vVec[2] * (j - imageCenter)
                            + viewVec[2]*k + volumeCenter[2];
                    // INTERPOLATE HERE!!!!!!
                    int val = interpolateVoxel(pixelCoord);
                    //int val = getVoxel(pixelCoord);

                    // get color with TF
                    voxelColor = tFunc.getColor(val);

                    compositeColor.r = voxelColor.r * voxelColor.a + (1.0 - voxelColor.a) * compositeColor.r;
                    compositeColor.g = voxelColor.g * voxelColor.a + (1.0 - voxelColor.a) * compositeColor.g;
                    compositeColor.b = voxelColor.b * voxelColor.a + (1.0 - voxelColor.a) * compositeColor.b;

                }

                // BufferedImage expects a pixel color packed as ARGB in an int
                int c_alpha = compositeColor.a <= 1.0 ? (int) Math.floor(compositeColor.a * 255) : 255;
                int c_red = compositeColor.r <= 1.0 ? (int) Math.floor(compositeColor.r * 255) : 255;
                int c_green = compositeColor.g <= 1.0 ? (int) Math.floor(compositeColor.g * 255) : 255;
                int c_blue = compositeColor.b <= 1.0 ? (int) Math.floor(compositeColor.b * 255) : 255;
                int pixelColor = (c_alpha << 24) | (c_red << 16) | (c_green << 8) | c_blue;
                image.setRGB(i, j, pixelColor);
            }
        }
    }

/////////////////////////////////////////////////////////////////////////////////////////////////

    //2Dtransfer function
    void twoDTransferFunction(double[] viewMatrix) {
        // clear image
        clear();


        //2Dtransfer function parameters
        double radius = tfEditor2D.triangleWidget.radius;
        double intensity = tfEditor2D.triangleWidget.baseIntensity;
        TFColor color = tfEditor2D.triangleWidget.color;
        double max = 114;
        double min = 0;

        // new variables
        TFColor voxelColor;
        TFColor compositeColor;

        // vector uVec and vVec define a plane through the origin,
        // perpendicular to the view vector viewVec
        double[] viewVec = new double[3];
        double[] uVec = new double[3];
        double[] vVec = new double[3];
        VectorMath.setVector(viewVec, viewMatrix[2], viewMatrix[6], viewMatrix[10]);
        VectorMath.setVector(uVec, viewMatrix[0], viewMatrix[4], viewMatrix[8]);
        VectorMath.setVector(vVec, viewMatrix[1], viewMatrix[5], viewMatrix[9]);

        // image is square
        int imageCenter = image.getWidth() / 2;
        double diagonal = volume.getDiagonal();

        double[] pixelCoord = new double[3];
        double[] volumeCenter = new double[3];
        VectorMath.setVector(volumeCenter, volume.getDimX() / 2, volume.getDimY() / 2, volume.getDimZ() / 2);

        // step interactive
        int step = (interactiveMode) ? stepInteractive : 1;

        for (int j = 0; j < image.getHeight(); j++) {
            for (int i = 0; i < image.getWidth(); i++) {
                // initialize color
                compositeColor = new TFColor(0,0,0,1);
                for(double k = -diagonal/2; k < diagonal/2; k+=step) {

                    pixelCoord[0] = uVec[0] * (i - imageCenter) + vVec[0] * (j - imageCenter)
                            + viewVec[0]*k + volumeCenter[0];
                    pixelCoord[1] = uVec[1] * (i - imageCenter) + vVec[1] * (j - imageCenter)
                            + viewVec[1]*k + volumeCenter[1];
                    pixelCoord[2] = uVec[2] * (i - imageCenter) + vVec[2] * (j - imageCenter)
                            + viewVec[2]*k + volumeCenter[2];
                    // INTERPOLATE HERE!!!!!!

                    int val = interpolateVoxel(pixelCoord);

                    //int val = getVoxel(pixelCoord);

                    // get color with TF
                    voxelColor =  new TFColor(color.r,color.g,color.b,color.a);
                    //voxelColor = phong(voxelColor, pixelCoord);
                    if(phongFlag){//phone_flag initializer is false
                        voxelColor = phong(voxelColor,viewVec,pixelCoord);
                    }

                   //voxelColor = phong(voxelColor,viewVec,pixelCoord);

                    float mag = interpolateGradient(pixelCoord);
                    //float mag = getGradient(pixelCoord);

                    if(mag == 0 && val == intensity)
                    {
                        voxelColor.a = color.a * 1;//color.a (alphaV)
                    //}else if((Math.abs(mag) > min) && (Math.abs(mag) <= max) && ((val - radius*mag) <= intensity) && ((val + radius*mag) >= intensity)){
                    }else if((Math.abs(mag) > 0) && ((val - radius*mag) <= intensity) && ((val + radius*mag) >= intensity)){
                        voxelColor.a = color.a * (1 - (1/radius)*(Math.abs((intensity-val)/mag)));
                    }else{
                        voxelColor.a = color.a * 0;
                    }



                    compositeColor.r = voxelColor.r * voxelColor.a + (1.0 - voxelColor.a) * compositeColor.r;
                    compositeColor.g = voxelColor.g * voxelColor.a + (1.0 - voxelColor.a) * compositeColor.g;
                    compositeColor.b = voxelColor.b * voxelColor.a + (1.0 - voxelColor.a) * compositeColor.b;

                }

                // BufferedImage expects a pixel color packed as ARGB in an int
                int c_alpha = compositeColor.a <= 1.0 ? (int) Math.floor(compositeColor.a * 255) : 255;
                int c_red = compositeColor.r <= 1.0 ? (int) Math.floor(compositeColor.r * 255) : 255;
                int c_green = compositeColor.g <= 1.0 ? (int) Math.floor(compositeColor.g * 255) : 255;
                int c_blue = compositeColor.b <= 1.0 ? (int) Math.floor(compositeColor.b * 255) : 255;
                int pixelColor = (c_alpha << 24) | (c_red << 16) | (c_green << 8) | c_blue;
                image.setRGB(i, j, pixelColor);
            }
        }
    }


    private void drawBoundingBox(GL2 gl) {
        gl.glPushAttrib(GL2.GL_CURRENT_BIT);
        gl.glDisable(GL2.GL_LIGHTING);
        gl.glColor4d(1.0, 1.0, 1.0, 1.0);
        gl.glLineWidth(1.5f);
        gl.glEnable(GL.GL_LINE_SMOOTH);
        gl.glHint(GL.GL_LINE_SMOOTH_HINT, GL.GL_NICEST);
        gl.glEnable(GL.GL_BLEND);
        gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);

        gl.glBegin(GL.GL_LINE_LOOP);
        gl.glVertex3d(-volume.getDimX() / 2.0, -volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(-volume.getDimX() / 2.0, volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, -volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glEnd();

        gl.glBegin(GL.GL_LINE_LOOP);
        gl.glVertex3d(-volume.getDimX() / 2.0, -volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glVertex3d(-volume.getDimX() / 2.0, volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, -volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glEnd();

        gl.glBegin(GL.GL_LINE_LOOP);
        gl.glVertex3d(volume.getDimX() / 2.0, -volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, -volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glEnd();

        gl.glBegin(GL.GL_LINE_LOOP);
        gl.glVertex3d(-volume.getDimX() / 2.0, -volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glVertex3d(-volume.getDimX() / 2.0, -volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(-volume.getDimX() / 2.0, volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(-volume.getDimX() / 2.0, volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glEnd();

        gl.glBegin(GL.GL_LINE_LOOP);
        gl.glVertex3d(-volume.getDimX() / 2.0, volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glVertex3d(-volume.getDimX() / 2.0, volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glEnd();

        gl.glBegin(GL.GL_LINE_LOOP);
        gl.glVertex3d(-volume.getDimX() / 2.0, -volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glVertex3d(-volume.getDimX() / 2.0, -volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, -volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, -volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glEnd();

        gl.glDisable(GL.GL_LINE_SMOOTH);
        gl.glDisable(GL.GL_BLEND);
        gl.glEnable(GL2.GL_LIGHTING);
        gl.glPopAttrib();

    }

    @Override
    public void visualize(GL2 gl) {

        if (volume == null) {
            return;
        }

        drawBoundingBox(gl);

        gl.glGetDoublev(GL2.GL_MODELVIEW_MATRIX, viewMatrix, 0);

        long startTime = System.currentTimeMillis();

        switch(method) {
            case SLICER:
                tf2dFlag = false;
                slicer(viewMatrix);
                break;
            case MIP:
                tf2dFlag = false;
                mip(viewMatrix);
                break;
            case COMPOSITING:
                tf2dFlag = false;
                compositing(viewMatrix);
                break;
            case TF2D:
                tf2dFlag = true;
                twoDTransferFunction(viewMatrix);
                break;
            case PHONG:
                toggle();
                //System.out.println("Cambio a phong o no: " + phongFlag);
                if(tf2dFlag) {
                    twoDTransferFunction(viewMatrix);
                }
                break;
        }
        
        long endTime = System.currentTimeMillis();
        double runningTime = (endTime - startTime);
        panel.setSpeedLabel(Double.toString(runningTime));

        Texture texture = AWTTextureIO.newTexture(gl.getGLProfile(), image, false);

        gl.glPushAttrib(GL2.GL_LIGHTING_BIT);
        gl.glDisable(GL2.GL_LIGHTING);
        gl.glEnable(GL.GL_BLEND);
        gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);

        // draw rendered image as a billboard texture
        texture.enable(gl);
        texture.bind(gl);
        double halfWidth = image.getWidth() / 2.0;
        gl.glPushMatrix();
        gl.glLoadIdentity();
        gl.glBegin(GL2.GL_QUADS);
        gl.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        gl.glTexCoord2d(0.0, 0.0);
        gl.glVertex3d(-halfWidth, -halfWidth, 0.0);
        gl.glTexCoord2d(0.0, 1.0);
        gl.glVertex3d(-halfWidth, halfWidth, 0.0);
        gl.glTexCoord2d(1.0, 1.0);
        gl.glVertex3d(halfWidth, halfWidth, 0.0);
        gl.glTexCoord2d(1.0, 0.0);
        gl.glVertex3d(halfWidth, -halfWidth, 0.0);
        gl.glEnd();
        texture.disable(gl);
        texture.destroy(gl);
        gl.glPopMatrix();

        gl.glPopAttrib();


        if (gl.glGetError() > 0) {
            System.out.println("some OpenGL error: " + gl.glGetError());
        }

    }
    private BufferedImage image;
    private double[] viewMatrix = new double[4 * 4];

    @Override
    public void changed() {
        for (int i=0; i < listeners.size(); i++) {
            listeners.get(i).changed();
        }
    }
}
