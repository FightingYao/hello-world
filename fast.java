public byte[] encode(ScalarValue value) {        
        byte[] encoding = encodeValue(value);
        encoding[encoding.length - 1] |= 0x80; 
        return encoding;
	}
	
	/**
     * 编码方法
     * */
    public byte[] encodeValue(ScalarValue value) {    
        long longValue = ((NumericValue) value).toLong();        
        int size = getSignedIntegerSize(longValue);       
        byte[] encoding = new byte[size];        //组装数据，即每个字节第一位不表示数据；组装完成后仍然是大端序列(低字节位为值得高有效位)
        for (int factor = 0; factor < size; factor++) {            //0x3f = 0011 1111
            //0x7f = 0111 1111
            int bitMask = (factor == (size - 1)) ? 0x3f : 0x7f;
            encoding[size - factor - 1] = (byte) ((longValue >> (factor * 7)) & bitMask);
        }        // Get the sign bit from the long value and set it on the first byte
        // 01000000 00000000 ... 00000000
        // ^----SIGN BIT
        //将一个字节的第二位设置为符号位， 0表示正数；1表示负数
        encoding[0] |= (0x40 & (longValue >> 57));        return encoding;
    }    /**
     * 解码方法
     * */
    public ScalarValue decode(InputStream in) {       
        long value = 0;        
        try {            // IO read方法如果返回小于-1的时候，表示结束；正常范围0-255
            int byt = in.read();           
            if (byt < 0) {
                Global.handleError(FastConstants.END_OF_STREAM, "The end of the input stream has been reached.");             
                return null; // short circuit if global error handler does not throw exception
            }            //通过首字节的第二位与运算，确认该数据的符号
            if ((byt & 0x40) > 0) { 
                value = -1;
            }            //到此，value的符号已经确定，
            //value=0 则该数为负数， value= -1该数为正数
            // int value = -1   16进制为 0xFF FF FF FF
            // int value = 0    16进制为 0x00 00 00 00
            //下面的只是通过位操作来复原真实的数据
            value = (value << 7) | (byt & 0x7f);  //(value << 7)确保最后7位为0；     (byt & 0x7f) 还是byt
            while ((byt & 0x80) == 0) {  //根据第一位来判断当前byte是否属于这个字段
                byt = in.read();              
                if (byt < 0) {
                    Global.handleError(FastConstants.END_OF_STREAM, "The end of the input stream has been reached.");                   
                    return null; // short circuit if global error handler does not throw exception
                }               
                value = (value << 7) | (byt & 0x7f); //先把有效位往左移7位，然后再处理当前的七位
            }
        } catch (IOException e) {
            Global.handleError(FastConstants.IO_ERROR, "A IO error has been encountered while decoding.", e);      
            return null; // short circuit if global error handler does not throw exception
        }        
        return createValue(value);
    }    /**
     * 判断无符号数所要占用的字节数
     * */
    public static int getUnsignedIntegerSize(long value) {    
        if (value < 128) {        
            return 1; // 2 ^ 7
        }        
        if (value <= 16384) {         
            return 2; // 2 ^ 14
        }       
        if (value <= 2097152) {           
            return 3; // 2 ^ 21
        }      
        if (value <= 268435456) {       
            return 4; // 2 ^ 28
        }      
        if (value <= 34359738368L) {            
            return 5; // 2 ^ 35
        }     
        if (value <= 4398046511104L) {          
            return 6; // 2 ^ 42
        }      
        if (value <= 562949953421312L) {    
            return 7; // 2 ^ 49
        }     
        if (value <= 72057594037927936L) {    
            return 8; // 2 ^ 56
        }      
        return 9;
    }    /**
     * 判断有符号数需要占用的字节
     * */
    public static int getSignedIntegerSize(long value) {      
        if ((value >= -64) && (value <= 63)) {       
            return 1; // - 2 ^ 6 ... 2 ^ 6 -1
        }     
        if ((value >= -8192) && (value <= 8191)) {         
            return 2; // - 2 ^ 13 ... 2 ^ 13 -1
        }       
        if ((value >= -1048576) && (value <= 1048575)) {          
            return 3; // - 2 ^ 20 ... 2 ^ 20 -1
        }       
        if ((value >= -134217728) && (value <= 134217727)) {           
            return 4; // - 2 ^ 27 ... 2 ^ 27 -1
        }      
        if ((value >= -17179869184L) && (value <= 17179869183L)) {           
            return 5; // - 2 ^ 34 ... 2 ^ 34 -1
        }       
        if ((value >= -2199023255552L) && (value <= 2199023255551L)) {         
            return 6; // - 2 ^ 41 ... 2 ^ 41 -1
        }      
        if ((value >= -281474976710656L) && (value <= 281474976710655L)) {       
            return 7; // - 2 ^ 48 ... 2 ^ 48 -1
        }       
        if ((value >= -36028797018963968L) && (value <= 36028797018963967L)) {      
            return 8; // - 2 ^ 55 ... 2 ^ 55 -1
        }       
        if ((value >= -4611686018427387904L && value <= 4611686018427387903L)) {       
            return 9;
        }        
        return 10;
    }
