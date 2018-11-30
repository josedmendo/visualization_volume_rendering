# Visualization Project: Volume Rendering
## Ray casting
Study the method slicer() in the class RaycastRenderer, and use this as a basis to develop
a raycaster that supports both Maximum Intensity Projection and compositing ray functions. The
coordinate system of the view matrix is explained in the code. Furthermore, the code already
provides a transfer function editor so you can more easily specify colors and opacities to be
used by the compositing ray function. 
Implement the following functionalities:
1. Tri-linear interpolation of samples along the viewing ray.
2. MIP and compositing ray functions.

As you may notice, a software raycaster is quite slow. You can easily make the application
a bit more responsive by lowering the resolution during rendering (both in number of steps
along the view ray as in the number of pixels you sample). The RaycastRenderer already
has a variable interactiveMode which is set to true when you interact with the mouse, and
is set to false when interaction stops.

## 2-D transfer function
As discussed in the lectures, simple intensity-based transfer functions do not allow to highlight
all features in datasets. By incorporating gradient information in the transfer functions, you
gain additional possibilities.
Below are the requirements:
1. Implement gradient-based opacity weighting in your raycaster, as described in Levoy’s
paper [2] in the section entitled “Isovalue contour surfaces”, eq. (3). The code already
contains a 2-D transfer function editor which shows an intensity vs. gradient magnitude
plot, and provides a simple triangle widget to specify the parameters. The class
GradientVolume should be extended so that it actually computes the gradients.
2. Implement an illumination model, e.g., Phong shading as discussed in the lectures. An
example result with and without illumination is shown in Fig. 1.2. The Phong shading
parameters in this example are kambient = 0:1, kdiff = 0:7, kspec = 0:2, and alpha = 10.

## Bibliography
[1] Joe Kniss, G. L. Kindlmann, and C. D. Hansen. Multidimensional transfer functions for
interactive volume rendering. IEEE Trans. Visualization and Computer Graphics, 8(3):270–285,
2002.
[2] M. Levoy. Display of surfaces from volume data. IEEE Computer Graphics and Applications,
8(3):29–37, 1988.
