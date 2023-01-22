package com.apk.axml.utils;

public class TypedValue {

    public CharSequence string;
    public int data;
	
    public static final int
    	TYPE_REFERENCE = 1,
    	TYPE_ATTRIBUTE = 2,
    	TYPE_STRING = 3,
    	TYPE_FLOAT = 4,
    	TYPE_DIMENSION = 5,
    	TYPE_FRACTION = 6,
    	TYPE_FIRST_INT = 16,
    	TYPE_INT_HEX = 17,
    	TYPE_INT_BOOLEAN = 18,
    	TYPE_FIRST_COLOR_INT = 28,
    	TYPE_LAST_COLOR_INT = 31,
    	TYPE_LAST_INT = 31;
    
    public static final int
	    COMPLEX_UNIT_MASK		=15;
	
}