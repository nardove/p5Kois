import processing.core.*; 
import processing.xml.*; 

import processing.opengl.PGraphicsOpenGL; 

import java.applet.*; 
import java.awt.Dimension; 
import java.awt.Frame; 
import java.awt.event.MouseEvent; 
import java.awt.event.KeyEvent; 
import java.awt.event.FocusEvent; 
import java.awt.Image; 
import java.io.*; 
import java.net.*; 
import java.text.*; 
import java.util.*; 
import java.util.zip.*; 
import java.util.regex.*; 

public class koi_pond_v10 extends PApplet {

/*
KOY FISH POND
 by Ricardo Sanchez
 July 2009
 
 TODO:
 - fix opengl issue when using ripple class
 - check fish birth functions
 */



int NUM_BOIDS = 60;
int lastBirthTimecheck = 0;                // birth time interval
int addKoiCounter = 0;

ArrayList wanderers = new ArrayList();     // stores wander behavior objects
PVector mouseAvoidTarget;                  // use mouse location as object to evade
boolean press = false;                     // check is mouse is press
int mouseAvoidScope = 100;    

String[] skin = new String[10];

PImage canvas;
Ripple ripples;
boolean isRipplesActive = false;

PImage rocks;
PImage innerShadow;

public void setup() {
  size(900, 450, OPENGL);    // publish size
  //size(640, 480, OPENGL);      // blog size
  
  smooth();
  background(0);
  //frameRate(30);
  
  rocks = loadImage("rocks.jpg");
  innerShadow = loadImage("pond.png");
  
  // init skin array images
  for (int n = 0; n < 10; n++) skin[n] = "skin-" + n + ".png";

  // this is the ripples code
  canvas = createImage(width, height, RGB);
  ripples = new Ripple(canvas);
}


public void draw() {
  background(0);
  
  image(rocks, 0, 0);
  
  // adds new koi on a interval of time
  if (millis() > lastBirthTimecheck + 500) {
    lastBirthTimecheck = millis();
    if (addKoiCounter <  NUM_BOIDS) addKoi();
  }

  // fish motion wander behavior
  for (int n = 0; n < wanderers.size(); n++) {
    Boid wanderBoid = (Boid)wanderers.get(n);

    // if mouse is press pick objects inside the mouseAvoidScope
    // and convert them in evaders
    if (press) {
      if (dist(mouseX, mouseY, wanderBoid.location.x, wanderBoid.location.y) <= mouseAvoidScope) {
        wanderBoid.timeCount = 0;
        wanderBoid.evade(mouseAvoidTarget);
      }
    }
    else {
      wanderBoid.wander();
    }
    wanderBoid.run();
  }


  // ripples code
  if (isRipplesActive == true) {
    refreshCanvas();
    ripples.update();
  }
  
  image(innerShadow, 0, 0);
  
  //println("fps: " + frameRate);
}


// increments number of koi by 1
public void addKoi() {
  int id = PApplet.parseInt(random(1, 11)) - 1;
  wanderers.add(new Boid(skin[id],
  new PVector(random(100, width - 100), random(100, height - 100)),
  random(0.8f, 1.9f), 0.2f));
  Boid wanderBoid = (Boid)wanderers.get(addKoiCounter);
  // sets opacity to simulate deepth
  wanderBoid.maxOpacity = PApplet.parseInt(map(addKoiCounter, 0, NUM_BOIDS - 1, 10, 170));

  addKoiCounter++;
}


// use for the ripple effect to refresh the canvas
public void refreshCanvas() {
  loadPixels();
  System.arraycopy(pixels, 0, canvas.pixels, 0, pixels.length);
  updatePixels();
}



public void mousePressed() {
  press = true;
  mouseAvoidTarget = new PVector(mouseX, mouseY);

  if (isRipplesActive == true) ripples.makeTurbulence(mouseX, mouseY);
}

public void mouseDragged() {
  mouseAvoidTarget.x = mouseX;
  mouseAvoidTarget.y = mouseY;

  if (isRipplesActive == true) ripples.makeTurbulence(mouseX, mouseY);
}

public void mouseReleased() {
  press = false;
}


/*
Steer behavior class, to control/simulate natural movement
 the idea is to make some behaviors interactive like
 */

class Boid extends Flagellum {

  PVector location;
  PVector velocity;
  PVector acceleration;
  float r;
  float maxForce;
  float maxSpeed;
  float wandertheta;
  float rushSpeed = random(3, 6);

  boolean timeCheck = false;                 // check if time interval is complete
  int timeCount = 0;                         // time cicle index
  int lastTimeCheck = 0;                     // stores last time check
  int timeCountLimit = 10;                   // max time cicles


  Boid (String _skin, PVector _location, float _maxSpeed, float _maxForce) {
    super(_skin);

    location = _location.get();
    velocity = new PVector(0, 0);
    acceleration = new PVector(0, 0);
    maxForce = _maxForce;
    maxSpeed = _maxSpeed;
  }


  public PVector steer(PVector target, boolean slowdown) {
    PVector steer;
    PVector desired = PVector.sub(target, location);
    float d = desired.mag();

    if (d > 0) {
      desired.normalize();

      if (slowdown && d < 100) {
        desired.mult(maxSpeed * (d / 100));
      }
      else {
        desired.mult(maxSpeed);
      }

      steer = PVector.sub(desired, velocity);
      steer.limit(maxForce);
    }
    else {
      steer = new PVector(0, 0);
    }

    return steer;
  }


  /*  SEEK - FLEE  */
  public void seek(PVector target) {
    acceleration.add(steer(target, false));
  }

  public void arrive(PVector target) {
    acceleration.add(steer(target, true));
  }

  public void flee(PVector target) {
    acceleration.sub(steer(target, false));
  }



  /*  PURSUE - EVADE  */
  public void pursue(PVector target) {
    float lookAhead = location.dist(target) / maxSpeed;
    PVector predictedTarget = new PVector(target.x + lookAhead, target.y + lookAhead);
    seek(predictedTarget);
  }

  public void evade(PVector target) {
    timeCheck = true;
    if (dist(target.x, target.y, location.x, location.y) < 100) {
      float lookAhead = location.dist(target) / (maxSpeed * 2);
      PVector predictedTarget = new PVector(target.x - lookAhead, target.y - lookAhead);
      flee(predictedTarget);
    }
  }


  /*  WANDER  */
  public void wander() {
    float wanderR = 5;
    float wanderD = 100;
    float change = 0.05f;

    wandertheta += random(-change, change);

    PVector circleLocation = velocity.get();
    circleLocation.normalize();
    circleLocation.mult(wanderD);
    circleLocation.add(location);

    PVector circleOffset = new PVector(wanderR * cos(wandertheta), wanderR * sin(wandertheta));
    PVector target = PVector.add(circleLocation, circleOffset);
    seek(target);
  }


  public void run() {
    update();
    borders();
    display();
  }


  public void update() {
    velocity.add(acceleration);
    velocity.limit(maxSpeed);
    location.add(velocity);
    acceleration.mult(0);

    // sets flagellum muscleFreq in relation to velocity
    //super.muscleRange = norm(velocity.mag(), 0, 1) * 2.5;
    super.muscleFreq = norm(velocity.mag(), 0, 1) * 0.06f;
    super.move();

    if (timeCheck) {
      if (millis() > lastTimeCheck + 200) {
        lastTimeCheck = millis();

        if (timeCount <= timeCountLimit) {
          // derease maxSpeed in relation with time cicles
          // this formula needs a proper look
          maxSpeed = rushSpeed - (norm(timeCount, 0, timeCountLimit) * 3);
          timeCount++;
        }
        else if (timeCount >= timeCountLimit) {
          // once the time cicle is complete
          // resets timer variables,
          timeCount = 0;
          timeCheck = false;

          // set default speed values
          maxSpeed = random(0.8f, 1.9f);
          maxForce = 0.2f;
        }
      }
    }
  }


  // control skin tint, for now it picks a random dark grey color
  int opacity = 0;
  int maxOpacity = 0;

  public void display() {
    if (opacity < 255) opacity += 1;
    else opacity = 255;
    tint(maxOpacity, maxOpacity, maxOpacity, opacity);

    // update location and direction
    float theta = velocity.heading2D() + radians(180);
    pushMatrix();
    translate(location.x, location.y);
    //rotate(theta);
    super.display();
    popMatrix();
    noTint();

    // update flagellum body rotation
    super.theta = degrees(theta);
    super.theta += 180;
  }

  // wrapper, appear opposit side
  public void borders() {
    if (location.x < -skin.width) location.x = width;
    if (location.x > width + skin.width) location.x = 0;
    if (location.y < -skin.width) location.y = height;
    if (location.y > height + skin.width) location.y = 0;
  }

}





/*
    Fish locomotion class
    Logic from levitated.com, simulates wave propagation through a kinetic array of nodes
    also some bits from flight404 blog
*/
class Flagellum {

  int numNodes = 16;
  float skinXspacing, skinYspacing;          // store distance for vertex points that builds the shape
  float muscleRange = 6;                     // controls rotation angle of the neck
  float muscleFreq = random(0.06f, 0.07f);     // 
  float theta_vel;
  float theta = 180;
  float theta_friction = 0.6f;
  float count = 0;

  Node[] node = new Node[numNodes];

  PImage skin;
  
  
  Flagellum(String _skin) {
    skin = loadImage(_skin);
     
    // random image resize
    float scalar = random(0.5f, 1);
    skin.resize(PApplet.parseInt(skin.width * scalar), PApplet.parseInt(skin.height * scalar));
    
    // nodes spacing
    skinXspacing = skin.width / PApplet.parseFloat(numNodes) + 0.5f;
    skinYspacing = skin.height / 2;
    
    // initialize nodes
    for (int n = 0; n < numNodes; n++) node[n] = new Node();
    
  }
  

  public void move() {
    
    // head node
    node[0].x = cos(radians(theta));
    node[0].y = sin(radians(theta));

    // mucular node (neck)
    count += muscleFreq;
    float thetaMuscle = muscleRange * sin(count);
    node[1].x = -skinXspacing * cos(radians(theta + thetaMuscle)) + node[0].x;
    node[1].y = -skinXspacing * sin(radians(theta + thetaMuscle)) + node[0].y;

    // apply kinetic force trough body nodes (spine)
    for (int n = 2; n < numNodes; n++) {
      float dx = node[n].x - node[n - 2].x;
      float dy = node[n].y - node[n - 2].y;
      float d = sqrt(dx * dx + dy * dy);
      node[n].x = node[n - 1].x + (dx * skinXspacing) / d;
      node[n].y = node[n - 1].y + (dy * skinXspacing) / d;
    }
  }


  public void display() {
    noStroke();
    beginShape(QUAD_STRIP);
    texture(skin);
    for (int n = 0; n < numNodes; n++) {
      float dx;
      float dy;
      if (n == 0) {
        dx = node[1].x - node[0].x;
        dy = node[1].y - node[0].y;
      }
      else {
        dx = node[n].x - node[n - 1].x;
        dy = node[n].y - node[n - 1].y;
      }
      float angle = -atan2(dy, dx);
      float x1 = node[n].x + sin(angle) * -skinYspacing;
      float y1 = node[n].y + cos(angle) * -skinYspacing;
      float x2 = node[n].x + sin(angle) *  skinYspacing;
      float y2 = node[n].y + cos(angle) *  skinYspacing;
      float u = skinXspacing * n;
      vertex(x1, y1, u, 0);
      vertex(x2, y2, u, skin.height);
    }
    endShape();
  }
  
}

/*
  just stores x and y position, could be done in a different way but ...,
  will change in a future sketch
*/
class Node {
  float x;
  float y;
}

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



  public void update() {
    waterFilter(); 
    updateWater();
    page ^= 1; 
  }

  public void initMap() { 
    // the height map is made of two "pages" 
    // one to calculate the current state, and another to keep the previous state
    heightMap = new int[2][width][height]; 
   
  } 


  public void makeTurbulence(int cx, int cy) {
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


  public void waterFilter() { 
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

  public void updateWater() { 
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






  static public void main(String args[]) {
    PApplet.main(new String[] { "--bgcolor=#FFFFFF", "koi_pond_v10" });
  }
}
