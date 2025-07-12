# Mosaic Creator
A Java Swing application that allows users to create a mosaics from images. Simply drag image files to the window, crop/resize/split/re-arrange them as needed, and save the output!

### How to run:
* Just compile using your favorite build tool and run the main function inside mosaicmaker.MosaicMaker.java!

### Add image:
1) User can add image by dragging an image file into the window, or selecting File->Add image from top menu. 

### Select image: 
1) A user can select an image on the canvas by either clicking or right-clicking on it. 
2) When selected, the image will be outlined in blue. Each corner of the outline will have the resize handles.
3) Only one image can be selected at a time.
4) When an image is selected in the canvas, the following options in the Edit section of the top menu will become enabled. These options will also be accessible in the right-click menu for each image.
   * Delete image
   * Horizontal split 
   * Vertical split
   * Crop image

### Delete image:
1) User right-clicks on an image and selects "Delete image" in the popup menu
2) Alternatively, user can select the image by clicking on it, then select Edit->Delete Image option in the top menu bar. 
3) Alternatively, user can click to select an image and hit the delete button. 

### Split image
1) User right-clicks on an image and selects "Horizontal Split" or "Vertical Split" in the popup menu.
2) Alternatively, user can select the image by clicking on it, then select Edit->Horizontal Split/Vertical Split options in the top menu bar. 
3) A line spanning the width/height of the image will appear and the user will enter drag mode. The user will drag the line vertically to where the image will be split in 2. 
4) Once user exits drag mode, a confirmation prompt will appear. On confirm, the image will be split into two. 
5) To perform the split, the original image will be removed and be replaced by two new images representing the two halves.

### Crop image:
1) User right-clicks on an image and selects "crop" in the popup menu. 
2) Alternatively, user can select the image by clicking on it, then select Edit->Crop Image option in the top menu bar.  
3) The user will then create a crop rectangle within the image by clicking and dragging. 
4) Then, a menu will appear giving the option to apply the crop or cancel it.

## Bugs and other Todos:
1) Components cannot adjust when frame is in fullscreen mode.
2) Mosaic cannot be saved in full resolution, also all edits made are not present in output image. 
3) Need to add scroll panel so users can zoom and pan.