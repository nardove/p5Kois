/*
  how this works can be found here
  http://www.gamedev.net/reference/articles/article915.asp
  
  this end up as a simplified version of
  Riu Gil water sketch in openprocessing site
  http://www.openprocessing.org/visuals/?visualID=668
*/

class Ripple {

  int heightMap[][][];             // water surface (2 pages). 
  int turbulenceMap[][];           // turbulence map 
  int lineOptimizer[];             // line optimizer; 
  int space; 
  int radius, heightMax, density; 
  int page = 0; 

  PImage water;


  Ripple(PImage _water) {
    water = _water;
    density = 4; 
    radius = 20; 
    space = width * height - 1; 
    
    initMap();
  }



  void update() {
    waterFilter(); 
    updateWater();
    page ^= 1; 
  }

  void initMap() { 
    // the height map is made of two "pages" 
    // one to calculate the current state, and another to keep the previous state
    heightMap = new int[2][width][height]; 
   
  } 


  void makeTurbulence(int cx, int cy) {
    int r = radius * radius; 
    int left = cx < radius ? -cx + 1 : -radius; 
    int right = cx > (width - 1) - radius ? (width - 1) - cx : radius; 
    int top = cy < radius ? -cy + 1 : -radius; 
    int bottom = cy > (height - 1) - radius ? (height - 1) - cy : radius; 

    for (int x = left; x < right; x++) { 
      int xsqr = x * x; 
      for (int y = top; y < bottom; y++) { 
        if (xsqr + (y * y) < r)
          heightMap[page ^ 1][cx + x][cy + y] += 100;
      }
    } 
  }


  void waterFilter() { 
    for (int x = 0; x < width; x++) { 
      for (int y = 0; y < height; y++) { 
        int n = y - 1 < 0 ? 0 : y - 1; 
        int s = y + 1 > height - 1 ? height - 1 : y + 1; 
        int e = x + 1 > width - 1 ? width - 1 : x + 1; 
        int w = x - 1 < 0 ? 0 : x - 1; 
        int value = ((heightMap[page][w][n] + heightMap[page][x][n] 
          + heightMap[page][e][n] + heightMap[page][w][y] 
          + heightMap[page][e][y] + heightMap[page][w][s] 
          + heightMap[page][x][s] + heightMap[page][e][s]) >> 2) 
          - heightMap[page ^ 1][x][y];

        heightMap[page ^ 1][x][y] = value - (value >> density); 
      } 
    } 
  } 

  void updateWater() { 
    loadPixels();
    for (int y = 0; y < height - 1; y++) { 
      for (int x = 0; x < width - 1; x++) {
        int deltax = heightMap[page][x][y] - heightMap[page][x + 1][y]; 
        int deltay = heightMap[page][x][y] - heightMap[page][x][y + 1]; 
        int offsetx = (deltax >> 3) + x; 
        int offsety = (deltay >> 3) + y; 

        offsetx = offsetx > width ? width - 1 : offsetx < 0 ? 0 : offsetx; 
        offsety = offsety > height ? height - 1 : offsety < 0 ? 0 : offsety; 

        int offset = (offsety * width) + offsetx; 
        offset = offset < 0 ? 0 : offset > space ? space : offset;
        int pixel = water.pixels[offset]; 
        pixels[y * width + x] = pixel; 
      } 
    } 
    updatePixels(); 
  }

}





