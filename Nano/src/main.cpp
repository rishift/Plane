#include <Arduino.h>

#include <Servo.h>
#include <nRF24L01.h>
#include <RF24.h>

RF24 c(10, 9);
Servo t;
Servo e;
Servo r;

const byte a[] = {1, 1, 1};
byte d[2];

void setup()
{
    e.attach(7);
    r.attach(5);
    t.attach(6);

    e.write(90);
    r.write(99);
    t.writeMicroseconds(0);

    delay(1000);

    Serial.begin(9600);
    Serial.println("ok");

    Serial.println(c.begin());
    c.setChannel(120);
    c.setAddressWidth(3);
    c.setAutoAck(false);
    c.disableDynamicPayloads();
    c.setPayloadSize(2);
    c.setRadiation(RF24_PA_MAX, RF24_250KBPS, true);
    c.openReadingPipe(0, a);
    c.startListening();

}

void loop()
{
    if (c.available())
    {
        c.read(&d, 2);

        if (d[0] == 255)
        {
            if (d[1] == 0)
                t.writeMicroseconds(0);
            else
                t.writeMicroseconds(map(d[1], 1, 255, 200, 2000));
        }
        else
        {
            e.write(d[0]);
            r.write(d[1]);
        }

        Serial.print(d[0]);
        Serial.print(' ');
        Serial.println(d[1]);
    }
}