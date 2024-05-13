#include <Arduino.h>

#include <ESP8266WiFi.h>
#include <WiFiUdp.h>

#include <nRF24L01.h>
#include <RF24.h>

WiFiUDP t;
RF24 c(2, 4);

const byte a[] = {1, 1, 1};
byte d[2];

void setup()
{
    WiFi.mode(WIFI_AP);
    WiFi.softAP("Plane", "p l a n e", 12, false, 2, 1000);
    t.begin(11);

    Serial.begin(9600);
    Serial.println("ok");

    Serial.println(c.begin());
    c.setChannel(120);
    c.setAddressWidth(3);
    c.setAutoAck(false);
    c.disableDynamicPayloads();
    c.setPayloadSize(2);
    c.setRadiation(RF24_PA_MAX, RF24_250KBPS, true);
    c.openWritingPipe(a);
    c.stopListening();

}

void loop()
{
    if (t.parsePacket())
    {
        t.read(d, 2);
        Serial.printf("%u %u\n", d[0], d[1]);
        c.write(&d, 2);
    }
}