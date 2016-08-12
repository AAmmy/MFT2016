const int sp[] = { 
  0, 100, 200, 200};
int l, r, pre_l, pre_r;
const int o_l = 0, o_r = 0;
char mode, pre_mode = -1;
const int buttonPin1 = 14, buttonPin2 = 15;
int dmode;
void setup()
{
  Serial.begin(9600);
  initDCMotor();
  l = sp[0];
  r = sp[0];
  pinMode(buttonPin1, INPUT_PULLUP);
  pinMode(buttonPin2, INPUT_PULLUP);
  if(digitalRead(buttonPin1) == LOW)
    dmode = 1;
  if(digitalRead(buttonPin2) == LOW)
    dmode = 2;
}

void ch_led_(){
  if(mode == pre_mode)
    digitalWrite(13, LOW);
  else
    digitalWrite(13, HIGH);
  pre_mode = mode;
}

void ch_led(){
  if(pre_l == l && pre_r == r)
    digitalWrite(13, LOW);
  else
    digitalWrite(13, HIGH);
  pre_l = l;
  pre_r = r;
}

void loop() {
  ch_led();
  if ( Serial.available() > 0) {
    mode = Serial.read();
    if (mode != pre_mode && '0' <= mode && mode <= '9'){
      switch (mode) {
      case '0' :
        l = sp[2] + o_l;
        r = sp[2] + o_r;
        if(dmode == 1) r += random(60, 160);
        if(dmode == 2) l += random(60, 160);
        break;
      case '1' : 
        l = sp[3];
        r = sp[3];
        break;
      case '2' : 
        l = sp[0];
        r = sp[2];
        break;
      case '3' : 
        l = sp[1];
        r = sp[2];
        break;
      case '4' : 
        l = sp[2];
        r = sp[1];
        break;
      case '5' : 
        l = sp[2];
        r = sp[0];
        break;
      case '6' : 
        l = sp[3];
        r = sp[0];
        break;
      case '7' : 
        l = sp[0];
        r = sp[0];
        break;
      default :
        break;
      }
      setDCMotor(l, r);
      pre_mode = mode;
    }
  }
}






