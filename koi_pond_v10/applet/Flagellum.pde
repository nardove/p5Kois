/*
    Fish locomotion class
    Logic from levitated.com, simulates wave propagation through a kinetic array of nodes
    also some bits from flight404 blog
*/
class Flagellum {

  int numNodes = 16;
  float skinXspacing, skinYspacing;          // store distance for vertex points that builds the shape
  float muscleRange = 6;                     // controls rotation angle of the neck
  float muscleFreq = random(0.06, 0.07);     // 
  float theta_vel;
  float theta = 180;
  float theta_friction = 0.6;
  float count = 0;

  Node[] node = new Node[numNodes];

  PImage skin;
  
  
  Flagellum(String _skin) {
    skin = loadImage(_skin);
     
    // random image resize
    float scalar = random(0.5, 1);
    skin.resize(int(skin.width * scalar), int(skin.height * scalar));
    
    // nodes spacing
    skinXspacing = skin.width / float(numNodes) + 0.5;
    skinYspacing = skin.height / 2;
    
    // initialize nodes
    for (int n = 0; n < numNodes; n++) node[n] = new Node();
    
  }
  

  void move() {
    
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


  void display() {
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

