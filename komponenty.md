1. 15x TEPT4400 Phototransistor
2. 15x 5mm Red LED Diode
3. 7x 3-Position ON-OFF-ON Switch
4. STM32H7 
5. Moduł Bluetooth HM-10
6. ADAU1701 procesor DSP
7. Dotykowy wyświetlacz LCD TFT 2,4" SPI 320x240 ST7789


<!-- 1. 15x TEPT4400 Phototransistor
2. 15x 5mm Red LED Diode
3. 7x 3-Position ON-OFF-ON Switch
4. 2x ESP32-S3-DEV-KIT-N8R8  ? STM32 NUCLEO-F446ZE - STM32F446ZET6 ARM Cortex M4
5. Akumulator Li-Po 3.7V ? + przetwornica
6. Wyświetlacz 
7. ADAU1701 procesor DSP
8. karta SD do ESP
9. Głośnik -->



Opcja 2: Akumulator Li-ion 18650 + Przetwornica Step-Up
Aby uciągnąć 1.5A przez dłuższy czas, potrzebujesz dwóch ogniw 18650 połączonych równolegle lub jednego wysokoprądowego.
Użyj przetwornicy Pololu 5V 2.5A lub popularnej XL6009, aby utrzymać stabilne 5V nawet gdy bateria słabnie.



Piny "Wrażliwe" przy włączonym Bluetooth/Wi-Fi
W oryginalnym ESP32 (WROOM) używanie Wi-Fi blokowało piny ADC2. W ESP32-S3 ten problem został rozwiązany.
Możesz używać wszystkich pinów analogowych (ADC1 i ADC2) nawet gdy Bluetooth jest aktywny.
Jednak Ty używasz MCP3208 przez SPI, co jest jeszcze bezpieczniejsze, bo komunikacja cyfrowa SPI jest całkowicie niezależna od radia Bluetooth.





Sposoby na przyśpieszenie działania:

MCP3208 - moduł do odczytu wejść analogowych działający po protokole SPI
DMA - moduł na ESP32 alternatywa do MCP, zapisuje od razu pomiary do RAM
Zmniejszenie rezystora obciążeniowego na fototranzystorze 