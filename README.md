# BBQBuddy
an Arduino based, DYI Bluetooth BBQ thermometer with 2 probes (supports up to 4) and a companion Android App.

<img src="/app/src/main/res/mipmap-xxxhdpi/bbq.png">

## Overview
### Hardware
<img src="/Media/casing.jpg">

### Android App
<img src="/Media/current.png" width="250">
<img src="/Media/graph_portrait.png" width="250">
<img src="/Media/menu.png" width="250">
<img src="/Media/settings.png" width="250">

## Hardware 
### Schematic
<img src="/Media/bbqbuddy_schematic.jpg">
(Note: Fritzing did not have the exact same modules I was using. See the BOM below for the exact list of parts)

### AVR Code
* Follow Sparkfun's hookup guide to install the board file for Arduino IDE
  * https://learn.sparkfun.com/tutorials/pro-micro--fio-v3-hookup-guide
* Load the Arduino code
  * First, copy the following libraries to your libraries folder
  * [Libraries](/Arduino/libraries)
  * Then, load the project's AVR code
  * [AVR Code](/Arduino/bbqbuddy.ino)
* Connect the micro via USB, compile and transfer

### Bill of Materials
AVR
* Sparkfun Arduino Pro Micro - 5v/16Mhz
* Make sure to bridge jumper J1, we will be using a separate power supply, no need to waste energy and go through the internal 5v regulator 
  * Background: http://dlnmh9ip6v2uc.cloudfront.net/datasheets/Dev/Arduino/Boards/Pro_Micro_v13.pdf
* https://www.sparkfun.com/products/12640

Multiplexing 7-Segment Display
* LiteOn LTC-5723wc-10, or similar (options are plentiful, you want the multiplex version with decimal point)
* http://www.datasheetlib.com/datasheet/692818/ltc-5723wc-10_lite-on-technology/download.html

HC-05 Bluetooth Module
* Find these on Ebay or AliExpress

DC-DC Step-Up Converter
* Pololu U1V10F5
* https://www.pololu.com/product/2564

Maverick ET-72/73 Heat Sensors (Thermistor)
* You need two of those
* Buy them as replacement parts for the Maverick ET-71, ET-72 or ET-73 BBQ thermometer
* Easy to find in your local BBQ shop or on Amazon or Ebay
* Background: https://github.com/CapnBry/HeaterMeter/wiki/HeaterMeter-Probes

Passive elements
* 2x 100nF (ceramic)
* 1x 100uF (electrolyte)
* 2x 18k Resistor, **1%**
* 1x 10k Resistor, 5%
* 8x 330 Resistor, 5%
* 2x 2.5mm mono socket
* 2x AA NiMH cell
* 1x On/Off switch (print version)
* 1x Battery case for 2x AA cells
* 1x Your favorite stripboard, 2.54mm
* 8x M3 bolts, DIN 965, 10mm
* 8x Mainboard/Circuit-board spacer, M3, 15mm

### Casing
* TODO: CAD screenshot
* TODO: .dxf
* http://plexilaser.de/

