/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package volume;

/**
 *
 * @author michel
 */
public class GradientVolume {

    public GradientVolume(Volume vol) {
        gradientVolume = vol;
        dimX = vol.getDimX();
        dimY = vol.getDimY();
        dimZ = vol.getDimZ();
        GradientData = new VoxelGradient[dimX * dimY * dimZ];
        compute();
        maxmag = -1.0;
    }

    public VoxelGradient getGradient(int x, int y, int z) {
        return GradientData[x + dimX * (y + dimY * z)];
    }


    public void setGradient(int x, int y, int z, VoxelGradient value) {
        GradientData[x + dimX * (y + dimY * z)] = value;
    }

    public void setVoxel(int i, VoxelGradient value) {
        GradientData[i] = value;
    }

    public VoxelGradient getVoxel(int i) {
        return GradientData[i];
    }

    public int getDimX() {
        return dimX;
    }

    public int getDimY() {
        return dimY;
    }

    public int getDimZ() {
        return dimZ;
    }



    private void compute() {

        // this just initializes all gradients to the vector (0,0,0)
        for (int i=0; i<GradientData.length; i++) {
            GradientData[i] = zero;
        }

        float gradX,gradY,gradZ;
        for(int i=1; i< dimX-1;i++){
            for(int j=1; j< dimY-1;j++){
                for(int k=1; k< dimZ-1;k++){

                    gradX = (gradientVolume.getVoxel(i+1,j,k)-gradientVolume.getVoxel(i-1,j,k))/2;
                    gradY = (gradientVolume.getVoxel(i,j+1,k)-gradientVolume.getVoxel(i,j-1,k))/2;
                    gradZ = (gradientVolume.getVoxel(i,j,k+1)-gradientVolume.getVoxel(i,j,k-1))/2;

                    VoxelGradient val = new VoxelGradient(gradX,gradY,gradZ);

                    this.setGradient(i,j,k,val);
                }


            }
        }

    }


    public double getMaxGradientMagnitude() {
        if (maxmag >= 0) {
            return maxmag;
        } else {
            double magnitude = GradientData[0].mag;
            for (int i=0; i<GradientData.length; i++) {
                magnitude = GradientData[i].mag > magnitude ? GradientData[i].mag : magnitude;
            }
            maxmag = magnitude;
            return magnitude;
        }
    }

    private int dimX, dimY, dimZ;
    private VoxelGradient zero = new VoxelGradient();
    VoxelGradient[] GradientData;
    Volume gradientVolume;
    double maxmag;
}
