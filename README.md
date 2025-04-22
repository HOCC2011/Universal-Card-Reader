# Universal-Card-Reader
An android app that checkes the balance of different ic cards

# Octopus Cards
If you want the raw data collected from some Octopus cards, please read the files in the "Octopus Data" folder.

How to convert the raw value of service 0x0117 into readable card balance (16 bytes):
1. Take byte 2 and byte 3.
2. Join them into one number.
3. Subtract 350 from that number.
5. Divide the result by 10.
4. That gives the balance in Hong Kong Dollars (HKD).
Example:
If byte 2 = 0x06 and byte 3 = 0x5E, the number is 0x065E = 1630
Then:
 (1630 - 350) / 10 = 128.0 → that means the balance is HK$128.0

Difference between cards distributed before / after 1/10/2017:
Before: The balance is exactly same with the raw value
After: The balance is $15.0 less than the raw value

**How to calculate Octopus - China T-union card exchange rate:**
Exchange rate ≥ by 0.001 = T-union balance / Octopus balance (truncate to 3 decimal places)

# China T-Union Cards
Read the PPSE (Proximity Payment System Environment) of the card
All T-Union cards should contain an application called MOT_T_EP with the AID A000000632010105
The card number should be found in the response of selecting the application in the form of 03 10 4X XX XX XX XX XX XX XX
The balance can be read using the APDU command 80 5C 00 02 04  
It should reply 4 bytes of data in hexadecimal
The actual value needs to be divided by 100 because the returned value is in cents
