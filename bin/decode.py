from struct import *

with open("test_file.dat", "rb") as binary_file:
    data = binary_file.read(12)
    print(data)
    unpacked=unpack('!HHHHHH', data)
    print('ID: ', unpacked[0])

    print('QR: ',     (unpacked[1]&0x8000)>>15)
    print('Opcode: ', (unpacked[1]&0x7800)>>11)
    print('AA: ',     (unpacked[1]&0x0400)>>10)
    print('TC: ',     (unpacked[1]&0x0200)>>9)
    print('RD: ',     (unpacked[1]&0x0100)>>8)
    print('RA: ',     (unpacked[1]&0x0080)>>7)
    print('Z: ',      (unpacked[1]&0x0040)>>6)
    print('AD: ',     (unpacked[1]&0x0020)>>5)
    print('CD: ',     (unpacked[1]&0x0010)>>4)
    print('Rcode: ',  (unpacked[1]&0x000F))

    print('Questions: ', unpacked[2])
    print('Answers: ', unpacked[3])
    print('Authorities: ', unpacked[4])
    print('Additional: ', unpacked[5])
