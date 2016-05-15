# BBQBuddy
a Arduino based, DYI Bluetooth BBQ thermometer

<img src="/app/src/main/res/mipmap-xxxhdpi/bbq.png">

## Overview
### Android App
<img src="/Media/current.png" width="250">
<img src="/Media/graph_portrait.png" width="250">
<img src="/Media/menu.png" width="250">
<img src="/Media/settings.png" width="250">

### Hardware
<img src="/Media/casing.jpg">

## Build 
### Schematic
<img src="/Media/bbqbuddy_schematic.jpg">
(Note: Fritzing did not have the exact same modules I was using. See the BOM below for the exact list of parts)

### Bill of Materials
AVR
* Sparkfun Arduino Pro Micro - 5v/16Mhz
* https://www.sparkfun.com/products/12640

Multiplexing 7-Segment Display
* LiteOn LTC-5723wc-10, or similar (options are plentiful, you want the multiplex version with decimal point)
* http://www.datasheetlib.com/datasheet/692818/ltc-5723wc-10_lite-on-technology/download.html

HC-05 Bluetooth Module
* Find these on Ebay or AliExpress

DC-DC Converter
* Pololu U1V10F5
* https://www.pololu.com/product/2564

Maverick ET-72/73 Heat Sensors (Thermistor)
* You can buy them as replacement parts for the Maverick ET-71, ET-72 or ET-73 BBQ Thermometer
* Easy to find in your local BBQ shop or on Amazon or Ebay
* Background: https://github.com/CapnBry/HeaterMeter/wiki/HeaterMeter-Probes

Passive elements
* 2x 100nF (ceramic)
* 1x 100uF (electrolyte)
* 2x Resistor 18k, **1%**
* 1x 10k, 5%
* 8x 330, 5%
* 2x 2.5mm mono socket
* 2x AA NiMH cell
* 1x On-Off switch (print version)
* 1x Battery case for 2x AA NiMH

### Case
* TODO: CAD screen
* TODO: link to .dxf
* http://plexilaser.de/

