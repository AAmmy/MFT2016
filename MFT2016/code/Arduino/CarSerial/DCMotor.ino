const int DCMotorPIN[] = {5, 6};
int i_dcm;

void initDCMotor() {
  for (i_dcm = 0; i_dcm < 2; i_dcm++)
    pinMode(DCMotorPIN[i_dcm], OUTPUT);
}

void setDCMotor(int l_dcm, int r_dcm) {
    analogWrite(DCMotorPIN[0], l_dcm);
    analogWrite(DCMotorPIN[1], r_dcm);
}

