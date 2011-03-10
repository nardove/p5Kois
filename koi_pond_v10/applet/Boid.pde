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


  PVector steer(PVector target, boolean slowdown) {
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
  void seek(PVector target) {
    acceleration.add(steer(target, false));
  }

  void arrive(PVector target) {
    acceleration.add(steer(target, true));
  }

  void flee(PVector target) {
    acceleration.sub(steer(target, false));
  }



  /*  PURSUE - EVADE  */
  void pursue(PVector target) {
    float lookAhead = location.dist(target) / maxSpeed;
    PVector predictedTarget = new PVector(target.x + lookAhead, target.y + lookAhead);
    seek(predictedTarget);
  }

  void evade(PVector target) {
    timeCheck = true;
    if (dist(target.x, target.y, location.x, location.y) < 100) {
      float lookAhead = location.dist(target) / (maxSpeed * 2);
      PVector predictedTarget = new PVector(target.x - lookAhead, target.y - lookAhead);
      flee(predictedTarget);
    }
  }


  /*  WANDER  */
  void wander() {
    float wanderR = 5;
    float wanderD = 100;
    float change = 0.05;

    wandertheta += random(-change, change);

    PVector circleLocation = velocity.get();
    circleLocation.normalize();
    circleLocation.mult(wanderD);
    circleLocation.add(location);

    PVector circleOffset = new PVector(wanderR * cos(wandertheta), wanderR * sin(wandertheta));
    PVector target = PVector.add(circleLocation, circleOffset);
    seek(target);
  }


  void run() {
    update();
    borders();
    display();
  }


  void update() {
    velocity.add(acceleration);
    velocity.limit(maxSpeed);
    location.add(velocity);
    acceleration.mult(0);

    // sets flagellum muscleFreq in relation to velocity
    //super.muscleRange = norm(velocity.mag(), 0, 1) * 2.5;
    super.muscleFreq = norm(velocity.mag(), 0, 1) * 0.06;
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
          maxSpeed = random(0.8, 1.9);
          maxForce = 0.2;
        }
      }
    }
  }


  // control skin tint, for now it picks a random dark grey color
  int opacity = 0;
  int maxOpacity = 0;

  void display() {
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
  void borders() {
    if (location.x < -skin.width) location.x = width;
    if (location.x > width + skin.width) location.x = 0;
    if (location.y < -skin.width) location.y = height;
    if (location.y > height + skin.width) location.y = 0;
  }

}





