package com;
import java.util.Set;
public class LL1Utils {
	
	//用于判断一个字符是否是终结符
	public static boolean isFinalChar(char ch) {
		if (ch >= 'A' &&  ch <= 'Z') {
			return false;
		} else {
			return true;
		}
	}
	
	//用于判断一个产生式右部是否都属于nullable集合 
	public static boolean isBelongToNull(String right, Set<Character> set) {
		for(int i = 0; i < right.length(); i++) {
			if( !set.contains(right.charAt(i)) ) {
				return false;
			}
		}
		return true;
	}
	
	//判断一个非终结符是否能推出ε
	public static boolean isBelongToNull(char ch, Set<Character> set) {
		if( set.contains(ch) ) {
			return true;
		}
		return false;
	}
	
}	
